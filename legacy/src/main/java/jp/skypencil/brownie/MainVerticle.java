package jp.skypencil.brownie;

import java.util.UUID;

import com.google.inject.Guice;
import com.google.inject.Injector;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
import io.vertx.rxjava.servicediscovery.spi.ServiceImporter;
import io.vertx.servicediscovery.consul.ConsulServiceImporter;
import jp.skypencil.brownie.event.VideoUploadedEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * An entry point to launch the set of verticles.
 * Responsible to configure &amp; launch other verticles.
 */
@Slf4j
public class MainVerticle extends AbstractVerticle {
    private ServiceDiscovery discovery;
    private String deploymentID;

    /**
     * An entry point for debugging
     */
    public static void main(String[] args) {
        // specify logging framework
        // http://vertx.io/docs/vertx-core/java/#_logging
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");

        io.vertx.core.Vertx.vertx().deployVerticle(new MainVerticle());
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        getVertx().eventBus().registerDefaultCodec(UUID.class, new UuidCodec());
        getVertx().eventBus().registerDefaultCodec(VideoUploadedEvent.class, new VideoUploadedEventCodec());

        Injector injector = createInjector();
        DeploymentOptions options = new DeploymentOptions().setConfig(config());
        RxHelper.deployVerticle(vertx, injector.getInstance(FrontendServer.class), options)
            .subscribe(deploymentID -> {
                this.deploymentID = deploymentID;
                log.info("FrontendServer started");
                startFuture.complete();
            }, startFuture::fail);
    }

    private Injector createInjector() {
        discovery = ServiceDiscovery.create(vertx)
                .registerServiceImporter(new ServiceImporter(new ConsulServiceImporter()), new JsonObject()
                    .put("host", config().getString("BROWNIE_CONSUL_HOST", "localhost"))
                    .put("port", config().getInteger("BROWNIE_CONSUL_PORT", 8500))
                    .put("scan-period", 2000));

        Injector injector = Guice.createInjector(new BrownieModule(vertx, discovery));
        return injector;
    }

    @Override
    public void stop(Future<Void> stopFuture) {
        if (deploymentID == null) {
            throw new IllegalStateException("FrontendServer is not started yet");
        }
        vertx.undeployObservable(deploymentID)
                .subscribe(stopFuture::complete, stopFuture::fail);
    }
}
