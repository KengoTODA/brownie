package jp.skypencil.brownie;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.name.Names;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.asyncsql.AsyncSQLClient;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
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
    private final String mountedDir;
    private final ServiceDiscovery discovery;

    @Override
    protected void configure() {
        install(new CommonModule());

        bind(Vertx.class).toInstance(vertx);
        bind(AsyncSQLClient.class).toInstance(sqlClient);
        bind(ServiceDiscovery.class).toInstance(discovery);

        bind(FileMetadataRegistry.class).to(FileMetadataRegistryOnPostgres.class);
        bind(ThumbnailMetadataRegistry.class).to(ThumbnailMetadataRegistryOnPostgres.class);
        bind(VideoUploadedEventRegistry.class).to(VideoUploadedEventRegistryOnPostgres.class);
        bind(FileEncoder.class).to(FileEncoderFFmpeg.class);
        bind(ThumbnailGenerator.class).to(ThumbnailGeneratorFFmpeg.class);

        bind(String.class).annotatedWith(Names.named("mountedDir")).toInstance(mountedDir);
    }
}
