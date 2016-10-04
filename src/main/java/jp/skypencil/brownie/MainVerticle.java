package jp.skypencil.brownie;

import java.util.Set;
import java.util.UUID;

import com.google.inject.Guice;
import com.google.inject.Injector;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.dns.DnsClient;
import io.vertx.rxjava.ext.asyncsql.AsyncSQLClient;
import io.vertx.rxjava.ext.asyncsql.PostgreSQLClient;
import jp.skypencil.brownie.event.VideoUploadedEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * An entry point to launch the set of verticles.
 * Responsible to configure &amp; launch other verticles.
 */
@Slf4j
public class MainVerticle extends AbstractVerticle {
    private AsyncSQLClient sqlClient;
    private DnsClient dnsClient;

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

        String dnsHost = config().getString("BROWNIE_DNS_HOST", "localhost");
        Integer dnsPort = config().getInteger("BROWNIE_DNS_PORT", 53);
        log.info("Creating DnsClient for {}:{}", dnsHost, dnsPort);
        dnsClient = vertx.createDnsClient(dnsPort, dnsHost);

        String mountedDir = config().getString("BROWNIE_MOUNTED_DIR", System.getProperty("java.io.tmpdir", "/tmp"));
        log.info("Initializing module to use {} as temporal directory", mountedDir);

        Injector injector = Guice.createInjector(new BrownieModule(vertx, sqlClient, dnsClient, mountedDir));
        return injector;
    }

    @Override
    public void stop() throws Exception {
        sqlClient.close();

        Set<String> deploymentIDs = getVertx().deploymentIDs();
        deploymentIDs.forEach(deploymentID -> {
            getVertx().undeploy(deploymentID);
        });
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
