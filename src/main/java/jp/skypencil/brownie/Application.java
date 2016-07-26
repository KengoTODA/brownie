package jp.skypencil.brownie;

import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.DnsClient;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import jp.skypencil.brownie.fs.MountedFileSystem;
import jp.skypencil.brownie.fs.SharedFileSystem;
import jp.skypencil.brownie.registry.FileMetadataRegistry;
import jp.skypencil.brownie.registry.FileMetadataRegistryOnPostgres;
import jp.skypencil.brownie.registry.TaskRegistry;
import jp.skypencil.brownie.registry.TaskRegistryOnPostgres;
import lombok.extern.slf4j.Slf4j;

/**
 * An entry point to launch brownie server. To execute this class, simply run {@code java -jar}.
 */
@SpringBootApplication
@Slf4j
public class Application {
    @Value("${BROWNIE_CLUSTER_MODE:false}")
    private boolean clusterMode;

    /**
     * Host name to use for cluster mode. To enable cluster mode, user may specify this value by
     * {@code BROWNIE_CLUSTER_HOST} system property.
     */
    @Value("${BROWNIE_CLUSTER_HOST:}")
    private String clusterHost;

    @Value("${BROWNIE_MOUNTED_DIR:/mnt/brownie}")
    private String mountedDirectory;

    @Value("${BROWNIE_DNS_HOST:'localhost'}")
    private String dnsHost;

    @Value("${BROWNIE_DNS_PORT:53}")
    private int dnsPort;

    private Future<Vertx> vertxFuture;

    private Vertx vertx;

    public static void main(String[] args) {
        // specify logging framework
        // http://vertx.io/docs/vertx-core/java/#_logging
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");

        SpringApplication.run(Application.class, args);
    }

    /**
     * Initialize fields in this class.
     * 
     * This method kicks {@link Vertx#clusteredVertx(VertxOptions, io.vertx.core.Handler)} method,
     * to create a clustered instance asynchronously.
     */
    @PostConstruct
    public void prepareCluster() {
        if (!clusterMode) {
            log.info("STANDALONE mode: system property BROWNIE_CLUSTER_MODE is falsy");
            vertx = Vertx.vertx();
            vertxFuture = Future.succeededFuture(vertx);
            return;
        }

        ClusterManager mgr = new HazelcastClusterManager();
        VertxOptions options = new VertxOptions().setClusterManager(mgr);
        if (!clusterHost.isEmpty()) {
            options.setClusterHost(clusterHost);
        }
        log.info("CLUSTER mode: use {}:{} as host",
                options.getClusterHost(),
                options.getClusterPort());

        vertxFuture = Future.future();
        Vertx.clusteredVertx(options, result -> {
            if (result.failed()) {
                vertxFuture.fail(result.cause());
            } else {
                vertxFuture.complete(result.result());
            }
        });
    }

    @PreDestroy
    public void cleanUp() {
        if (vertx != null) {
            vertx.close();
        }
    }

    /**
     * Generate {@link Vertx} instance, based on given condition via system property.
     * 
     * @return {@link Vertx} instance to use in this application
     */
    @Bean
    public Vertx vertx() throws InterruptedException {
        while (!vertxFuture.isComplete()) {
            TimeUnit.MILLISECONDS.sleep(10);
        }
        if (vertxFuture.failed()) {
            throw new IllegalStateException("Failed to connect to cluster", vertxFuture.cause());
        }
        vertx = vertxFuture.result();
        vertx.eventBus().registerDefaultCodec(Task.class, new TaskCodec());
        return vertx;
    }

    /**
     * Generate {@link DnsClient} instance, to resolve SRV record for service discovery.
     *
     * @return {@link DnsClient} instance to use in this application
     */
    @Bean
    public DnsClient dnsClient() throws InterruptedException {
        log.info("Creating DnsClient for {}:{}", dnsHost, dnsPort);
        return vertx().createDnsClient(dnsPort, dnsHost);
    }

    @Bean
    public SharedFileSystem sharedFileSystem() {
        File directory = new File(mountedDirectory);
        if (!directory.isDirectory()) {
            throw new IllegalStateException("Specified directory does not exist: " + mountedDirectory);
        }
        log.info("Initialized shared file system at {}", mountedDirectory);
        return new MountedFileSystem(mountedDirectory);
    }

    @Bean
    public FileMetadataRegistry fileMetadataRegistry(Vertx vertx) {
        return new FileMetadataRegistryOnPostgres(vertx, postgresConfig());
    }

    @Bean
    public TaskRegistry taskRegistry(Vertx vertx) {
        return new TaskRegistryOnPostgres(vertx, postgresConfig());
    }

    private JsonObject postgresConfig() {
        return new JsonObject()
                .put("host", System.getProperty("db.host", "localhost"))
                .put("port", Integer.valueOf(System.getProperty("db.port", "5432")))
                .put("username", "brownie")
                .put("password", "brownie")
                .put("database", "brownie");
    }
}