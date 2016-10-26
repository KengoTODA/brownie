package jp.skypencil.brownie;

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
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileStorageMain extends AbstractVerticle {
    private AsyncSQLClient sqlClient;
    private FileStorageServer server;
    private ServiceDiscovery discovery;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        Injector injector = createInjector();
        server = injector.getInstance(FileStorageServer.class);
        DeploymentOptions options = new DeploymentOptions().setConfig(config());
        RxHelper.deployVerticle(vertx, server, options)
            .subscribe(record -> {
                log.info("FileStorageMain is started");
                startFuture.complete();
            }, startFuture::fail);
    }

    @Override
    public void stop(Future<Void> stopFuture) {
        vertx.undeployObservable(server.deploymentID())
            .flatMap(v -> {
                return sqlClient.closeObservable();
            })
            .toSingle()
            .subscribe(stopFuture::complete, stopFuture::fail);
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

        return Guice.createInjector(new FileStorageModule(vertx, sqlClient, discovery, mountedDir));
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
