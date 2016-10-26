package jp.skypencil.brownie.thumbnail;

import java.util.UUID;

import com.google.inject.Guice;
import com.google.inject.Injector;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.ext.asyncsql.AsyncSQLClient;
import io.vertx.rxjava.ext.asyncsql.PostgreSQLClient;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
import io.vertx.rxjava.servicediscovery.spi.ServiceImporter;
import io.vertx.servicediscovery.consul.ConsulServiceImporter;
import jp.skypencil.brownie.UuidCodec;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThumbnailMain extends AbstractVerticle {
    private AsyncSQLClient sqlClient;
    private ServiceDiscovery discovery;
    private ThumbnailServer server;

    @Override
    public void start(Future<Void> startFuture) {
        log.info("Start deploying thumbnail service...");

        Injector injector = createInjector();
        DeploymentOptions options = new DeploymentOptions().setConfig(config());
        server = injector.getInstance(ThumbnailServer.class);

        getVertx().eventBus().registerDefaultCodec(UUID.class, new UuidCodec());

        RxHelper.deployVerticle(vertx, server, options)
            .toSingle()
            .subscribe(deploymentID -> {
                log.info("thumbnail service has been deployed successfully. Its deploymentID is {}", deploymentID);
                startFuture.complete();
            }, startFuture::fail);
    }

    @Override
    public void stop(Future<Void> stopFuture) {
        log.info("Start stopping thumbnail service...");

        vertx.undeployObservable(server.deploymentID())
            .flatMap(v -> {
                return sqlClient.closeObservable();
            })
            .toSingle()
            .subscribe(v -> {
                log.info("thumbnail service has been undeployed successfully.");
                stopFuture.complete();
            }, stopFuture::fail);
    }

    private Injector createInjector() {
        JsonObject postgresConfig = createPostgresConfig();
        log.info("Creating PostgreSQLClient", postgresConfig);
        sqlClient = PostgreSQLClient.createShared(vertx, postgresConfig);

        discovery = ServiceDiscovery.create(vertx)
                .registerServiceImporter(new ServiceImporter(new ConsulServiceImporter()), new JsonObject()
                    .put("host", config().getString("BROWNIE_CONSUL_HOST", "localhost"))
                    .put("port", config().getInteger("BROWNIE_CONSUL_PORT", 8500))
                    .put("scan-period", 2000));

        Injector injector = Guice.createInjector(new ThumbnailModule(vertx, sqlClient, discovery));
        return injector;
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
