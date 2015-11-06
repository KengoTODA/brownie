package jp.skypencil.brownie;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * An entry point to launch brownie server. To execute this class, simply run {@code java -jar}.
 */
@SpringBootApplication
public class Application {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Host name to connect. To enable cluster mode, user should specify this value by
     * {@code BROWNIE_CLUSTER_HOST} system property.
     */
    @Value("${BROWNIE_CLUSTER_HOST:}")
    private String clusterHost;

    private Future<Vertx> vertxFuture;

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
        if (clusterHost.isEmpty()) {
            logger.info("STANDALONE mode: system property BROWNIE_CLUSTER_HOST not found");
            Vertx vertx = Vertx.vertx();
            vertxFuture = Future.succeededFuture(vertx);
            return;
        }

        logger.info("CLUSTER mode: use {} as host", clusterHost);

        ClusterManager mgr = new HazelcastClusterManager();
        VertxOptions options = new VertxOptions().setClusterManager(mgr);
        options.setClusterHost(clusterHost);

        vertxFuture = Future.future();
        Vertx.clusteredVertx(options, result -> {
            if (result.failed()) {
                vertxFuture.fail(result.cause());
            } else {
                vertxFuture.complete(result.result());
            }
        });
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
        Vertx vertx = vertxFuture.result();
        vertx.eventBus().registerDefaultCodec(Task.class, new TaskCodec());
        return vertx;
    }
}