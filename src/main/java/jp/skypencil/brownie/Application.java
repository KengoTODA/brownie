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

@SpringBootApplication
public class Application {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${BROWNIE_CLUSTER_HOST:}")
    private String clusterHost;

    private Future<Vertx> vertxFuture;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @PostConstruct
    public void prepareCluster() {
        if (clusterHost.isEmpty()) {
            logger.info("STANDALONE mode: environment variable BROWNIE_CLUSTER_HOST not found");
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