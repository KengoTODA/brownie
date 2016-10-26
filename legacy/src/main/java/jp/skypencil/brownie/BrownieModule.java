package jp.skypencil.brownie;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class BrownieModule extends AbstractModule implements Module {
    private final Vertx vertx;
    private final ServiceDiscovery discovery;

    @Override
    protected void configure() {
        install(new CommonModule());

        bind(Vertx.class).toInstance(vertx);
        bind(ServiceDiscovery.class).toInstance(discovery);
    }
}
