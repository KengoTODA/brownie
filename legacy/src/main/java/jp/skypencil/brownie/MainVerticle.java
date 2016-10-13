package jp.skypencil.brownie;

import java.util.Set;
import java.util.UUID;

import com.google.inject.Guice;
import com.google.inject.Injector;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.ext.asyncsql.AsyncSQLClient;
import io.vertx.rxjava.ext.asyncsql.PostgreSQLClient;
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
    private AsyncSQLClient sqlClient;
    private ServiceDiscovery discovery;

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
    public void start() throws Exception {
        log.info("MainVerticle started");

        getVertx().eventBus().registerDefaultCodec(UUID.class, new UuidCodec());
        getVertx().eventBus().registerDefaultCodec(VideoUploadedEvent.class, new VideoUploadedEventCodec());

        Injector injector = createInjector();
        DeploymentOptions options = new DeploymentOptions().setConfig(config());
        getVertx().deployVerticle(injector.getInstance(FrontendServer.class), options);
        getVertx().deployVerticle(injector.getInstance(EncodeServer.class), options);
        getVertx().deployVerticle(injector.getInstance(ThumbnailServer.class), options);
    }

    private Injector createInjector() {
        JsonObject postgresConfig = createPostgresConfig();
        log.info("Creating PostgreSQLClient", postgresConfig);
        sqlClient = PostgreSQLClient.createShared(vertx, postgresConfig);

        String mountedDir = config().getString("BROWNIE_MOUNTED_DIR", System.getProperty("java.io.tmpdir", "/tmp"));
        log.info("Initializing module to use {} as temporal directory", mountedDir);

        discovery = ServiceDiscovery.create(vertx)
                .registerServiceImporter(new ServiceImporter(new ConsulServiceImporter()), new JsonObject()
                    .put("host", config().getString("BROWNIE_CONSUL_HOST", "localhost"))
                    .put("port", config().getInteger("BROWNIE_CONSUL_PORT", 8500))
                    .put("scan-period", 2000));


        Injector injector = Guice.createInjector(new BrownieModule(vertx, sqlClient, mountedDir, discovery));
        return injector;
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        sqlClient.closeObservable()
            .toSingle()
            .map(v -> {
                Set<String> deploymentIDs = getVertx().deploymentIDs();
                deploymentIDs.forEach(deploymentID -> {
                    getVertx().undeploy(deploymentID);
                });
                return v;
            })
            .subscribe(stopFuture::complete, stopFuture::fail);
    }

    private JsonObject createPostgresConfig() {
        return new JsonObject()
                .put("host", config().getString("BROWNIE_POSTGRES_HOST", "localhost"))
                .put("port", config().getInteger("BROWNIE_POSTGRES_PORT", 5432))
                .put("username", "brownie")
                .put("password", "brownie")
                .put("database", "brownie");
    }
}
