package jp.skypencil.brownie.thumbnail;

import com.google.inject.AbstractModule;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.asyncsql.AsyncSQLClient;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
import jp.skypencil.brownie.CommonModule;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class ThumbnailModule extends AbstractModule {

    private final Vertx vertx;
    private final AsyncSQLClient sqlClient;
    private final ServiceDiscovery discovery;

    @Override
    protected void configure() {
        install(new CommonModule());

        bind(Vertx.class).toInstance(vertx);
        bind(AsyncSQLClient.class).toInstance(sqlClient);
        bind(ServiceDiscovery.class).toInstance(discovery);

        bind(ThumbnailMetadataRegistry.class).to(ThumbnailMetadataRegistryOnPostgres.class);
        bind(ThumbnailGenerator.class).to(ThumbnailGeneratorFFmpeg.class);
    }

}
