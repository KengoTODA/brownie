package jp.skypencil.brownie;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import jp.skypencil.brownie.fs.DistributedFileSystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Server {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String directory = createDirectory();
    @Resource
    private Vertx vertx;

    @Resource
    private DistributedFileSystem fileSystem;

    @Value("${BROWNIE_CLUSTER_HTTP_PORT:8080}")
    private int httpPort;

    private String createDirectory() {
        try {
            Path directory = Files.createTempDirectory("brownie");
            logger.debug("Directory to store file is created at {}", directory);
            return directory.toFile().getAbsolutePath();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

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
