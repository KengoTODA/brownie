package jp.skypencil.brownie;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import jp.skypencil.brownie.fs.DistributedFileSystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * <p>A server class for front-end features, including:</p>
 * <ul>
 *  <li>Form to upload video</li>
 *  <li>REST API</li>
 *  <li>Admin console</li>
 * </ul>
 *
 * <p>This class is responsible to map URL to related operations.</p>
 */
@Component
public class FrontendServer {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * Directory to store uploaded file.
     */
    private final String directory = createDirectory();
    @Resource
    private Vertx vertx;

    @Resource
    private DistributedFileSystem fileSystem;

    /**
     * TCP port number to connect. It is {@code 8080} by default, and configurable
     * via {@code BROWNIE_CLUSTER_HTTP_PORT} system property.
     */
    @Value("${BROWNIE_CLUSTER_HTTP_PORT:8080}")
    @Nonnegative
    private int httpPort;

    @Nonnull
    private String createDirectory() {
        try {
            Path directory = Files.createTempDirectory("brownie");
            logger.debug("Directory to store file is created at {}", directory);
            return directory.toFile().getAbsolutePath();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Create {@link HttpServer} to listen specified {@link #httpPort}.
     */
    @PostConstruct
    public void createServer(){
        Router router = Router.router(vertx);

        // Serve the static pages
        router.route().handler(StaticHandler.create());

        // Serve the form handling part
        router.route("/form").handler(BodyHandler.create().setUploadsDirectory(directory));
        router.post("/form").handler(ctx -> {
            ctx.fileUploads().forEach(fileUpload -> {
                Task task = new Task(fileUpload.uploadedFileName(), Collections.singleton("vga"));
                vertx.fileSystem().readFile(fileUpload.uploadedFileName(), read -> {
                    if (read.failed()) {
                        throw new RuntimeException("Failed to load file from file system", read.cause());
                    }
                    fileSystem.store(task.getKey(), read.result(), stored -> {
                        if (stored.failed()) {
                            throw new RuntimeException("Failed to store file onto file system", stored.cause());
                        }
                        vertx.eventBus().send("file-uploaded", task);
                    });
                });
            });
            ctx.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
                .end("registered");
        });
        vertx.createHttpServer().requestHandler(router::accept).listen(httpPort);
        logger.info("HTTP server is listening {} port", httpPort);
    }
}
