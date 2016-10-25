package jp.skypencil.brownie;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.asyncsql.AsyncSQLClient;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
import jp.skypencil.brownie.registry.VideoUploadedEventRegistry;
import jp.skypencil.brownie.registry.VideoUploadedEventRegistryOnPostgres;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class BrownieModule extends AbstractModule implements Module {
    private final Vertx vertx;
    private final AsyncSQLClient sqlClient;
    private final ServiceDiscovery discovery;

    @Override
    protected void configure() {
        install(new CommonModule());

        bind(Vertx.class).toInstance(vertx);
        bind(AsyncSQLClient.class).toInstance(sqlClient);
        bind(ServiceDiscovery.class).toInstance(discovery);

        bind(VideoUploadedEventRegistry.class).to(VideoUploadedEventRegistryOnPostgres.class);
        bind(FileEncoder.class).to(FileEncoderFFmpeg.class);
    }
}
