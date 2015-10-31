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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StaticServer {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String directory = createDirectory();
    private Vertx vertx;

    private String createDirectory() {
        try {
            Path directory = Files.createTempDirectory("brownie");
            logger.debug("Directory to store file is created at {}", directory);
            return directory.toFile().getAbsolutePath();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Resource
    public void setVertx(Vertx vertx) {
        this.vertx = vertx;
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
                vertx.eventBus().send("file-uploaded", task);
            });
            ctx.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
                .end("registered");
        });
        vertx.createHttpServer().requestHandler(router::accept).listen(8080);
    }
}
