package jp.skypencil.brownie;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.name.Names;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.dns.DnsClient;
import io.vertx.rxjava.ext.asyncsql.AsyncSQLClient;
import jp.skypencil.brownie.fs.MountedFileSystem;
import jp.skypencil.brownie.fs.SharedFileSystem;
import jp.skypencil.brownie.registry.FileMetadataRegistry;
import jp.skypencil.brownie.registry.FileMetadataRegistryOnPostgres;
import jp.skypencil.brownie.registry.ThumbnailMetadataRegistry;
import jp.skypencil.brownie.registry.ThumbnailMetadataRegistryOnPostgres;
import jp.skypencil.brownie.registry.VideoUploadedEventRegistry;
import jp.skypencil.brownie.registry.VideoUploadedEventRegistryOnPostgres;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class BrownieModule extends AbstractModule implements Module {
    private final Vertx vertx;
    private final AsyncSQLClient sqlClient;
    private final DnsClient dnsClient;
    private final String mountedDir;

    @Override
    protected void configure() {
        bind(Vertx.class).toInstance(vertx);
        bind(AsyncSQLClient.class).toInstance(sqlClient);
        bind(DnsClient.class).toInstance(dnsClient);

        bind(SharedFileSystem.class).to(MountedFileSystem.class);
        bind(FileMetadataRegistry.class).to(FileMetadataRegistryOnPostgres.class);
        bind(ThumbnailMetadataRegistry.class).to(ThumbnailMetadataRegistryOnPostgres.class);
        bind(VideoUploadedEventRegistry.class).to(VideoUploadedEventRegistryOnPostgres.class);
        bind(FileEncoder.class).to(FileEncoderFFmpeg.class);
        bind(ThumbnailGenerator.class).to(ThumbnailGeneratorFFmpeg.class);

        bind(IdGenerator.class).toInstance(new IdGenerator());
        bind(String.class).annotatedWith(Names.named("mountedDir")).toInstance(mountedDir);
    }
}
