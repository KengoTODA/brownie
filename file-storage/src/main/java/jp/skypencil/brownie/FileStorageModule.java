package jp.skypencil.brownie;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import io.vertx.rxjava.ext.asyncsql.AsyncSQLClient;
import io.vertx.rxjava.core.Vertx;
import jp.skypencil.brownie.fs.MountedFileSystem;
import jp.skypencil.brownie.fs.SharedFileSystem;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class FileStorageModule extends AbstractModule {
    private final Vertx vertx;
    private final AsyncSQLClient sqlClient;
    private final String mountedDir;

    @Override
    protected void configure() {
        install(new CommonModule());

        bind(Vertx.class).toInstance(vertx);
        bind(AsyncSQLClient.class).toInstance(sqlClient);
        bind(SharedFileSystem.class).to(MountedFileSystem.class);
        bind(String.class).annotatedWith(Names.named("mountedDir")).toInstance(mountedDir);
    }
}