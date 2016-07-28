package jp.skypencil.brownie;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import jp.skypencil.brownie.registry.ObservableFileMetadataRegistry;
import jp.skypencil.brownie.registry.ObservableTaskRegistry;
import lombok.extern.slf4j.Slf4j;



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
@Slf4j
public class FrontendServer {
    /**
     * Directory to store uploaded file.
     */
    private final String directory = createDirectory();
    @Resource
    private Vertx vertx;

    @Resource
    private FileTransporter fileTransporter;

    @Resource
    private ObservableTaskRegistry observableTaskRegistry;

    @Resource
    private ObservableFileMetadataRegistry observableFileMetadataRegistry;

    @Resource
    private KeyGenerator keyGenerator;

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
            log.debug("Directory to store file is created at {}", directory);
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

        // Serve the form handling part
        router.route("/form").handler(BodyHandler.create().setUploadsDirectory(directory));
        router.post("/form").handler(ctx -> {
            HttpServerResponse response = ctx.response()
                    .putHeader(HttpHeaders.CONTENT_TYPE, "text/plain");
            Set<FileUpload> uploadedFiles = ctx.fileUploads();
            if (uploadedFiles.isEmpty()) {
                response.end("No file uploaded");
                return;
            }
            AtomicInteger countDown = new AtomicInteger(uploadedFiles.size());
            uploadedFiles.forEach(fileUpload -> {
                Task task = new Task(keyGenerator.generateUuidV1(), fileUpload.fileName(), Collections.singleton("vga"));
                File file = new File(fileUpload.uploadedFileName());
                MimeType mimeType = MimeType.valueOf(fileUpload.contentType());
                fileTransporter.upload(task.getKey(), fileUpload.fileName(), file, mimeType, stored -> {
                    if (stored.failed()) {
                        log.warn("Failed to store file onto file system", stored.cause());
                        response
                            .setStatusCode(500)
                            .end("Internal server error");
                        return;
                    }
                    observableTaskRegistry.store(task).doOnCompleted(() -> {
                        vertx.eventBus().send("file-uploaded", task);
                        if (countDown.decrementAndGet() == 0) {
                            response.end("registered");
                        }
                    }).doOnError(error -> {
                        log.warn("Failed to store task to registry", error);
                        response
                            .setStatusCode(500)
                            .end("Internal server error");
                    }).subscribe();
                });
            });
        });
        router.mountSubRouter("/tasks", createRouterForTaskApi());
        router.mountSubRouter("/files", createRouterForFileApi());
        // Serve the static pages
        router.route().handler(StaticHandler.create());

        vertx.createHttpServer().requestHandler(router::accept).listen(httpPort);
        log.info("HTTP server is listening {} port", httpPort);
    }

    private Router createRouterForTaskApi() {
        Router subRouter = Router.router(vertx);
        subRouter.route().handler(ctx -> {
            HttpServerResponse response = ctx.response()
                    .setChunked(true)
                    .putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
                StringBuilder responseBody = new StringBuilder("[");
                observableTaskRegistry.iterate().subscribe(task -> {
                    if (responseBody.length() != 1) {
                        responseBody.append(",");
                    }
                    responseBody.append(task.toJson());
                }, error -> {
                    log.warn("Failed to load tasks", error);
                    response
                    .setStatusCode(500)
                    .end("Failed to load tasks");
                }, () -> {
                    response.end(responseBody.append("]").toString());
                });
        });
        return subRouter;
    }

    private Router createRouterForFileApi() {
        Router subRouter = Router.router(vertx);
        subRouter.get("/").handler(this::generateFileList);
        subRouter.get("/:fileId").handler(ctx -> {
            String fileId = ctx.request().getParam("fileId");
            downloadFile(ctx, fileId);
        });
        subRouter.delete("/:fileId").handler(ctx -> {
            String fileId = ctx.request().getParam("fileId");
            deleteFile(ctx, fileId);
        });
        return subRouter;
    }

    private void deleteFile(RoutingContext ctx, String fileId) {
        HttpServerResponse response = ctx.response();
        UUID key = UUID.fromString(fileId);
        fileTransporter.delete(key, deleted -> {
            if (deleted.succeeded()) {
                response.end("deleted");
            } else {
                response.setStatusCode(500).end("Failed to load file");
            }
        });
    }

    private void downloadFile(RoutingContext ctx, String fileId) {
        UUID key = UUID.fromString(fileId);
        HttpServerResponse response = ctx.response();
        observableFileMetadataRegistry.load(key).subscribe(metadata -> {
            long contentLength = metadata.getContentLength();
            response.putHeader("Content-Length", Long.toString(contentLength));
            fileTransporter.downloadToPipe(key, response, downloaded -> {
                if (downloaded.failed()) {
                    log.error("Failed to download file", downloaded.cause());
                    response.setStatusCode(500).end("Failed to load file");
                    return;
                }
                response.end();
            });
        }, error -> {
            log.error("Failed to download file", error);
            response.setStatusCode(500).end("Failed to load file");
        });
    }

    private void generateFileList(RoutingContext ctx) {
        HttpServerResponse response = ctx.response()
            .setChunked(true)
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        StringBuilder responseBody = new StringBuilder("[");
        observableFileMetadataRegistry.iterate().subscribe(metadata -> {
            if (responseBody.length() != 1) {
                responseBody.append(",");
            }
            responseBody.append(metadata.toJson());
        }, error -> {
            log.error("Failed to load files", error);
            response
                .setStatusCode(500)
                .end("Failed to load files");
        }, () -> {
            response.end(responseBody.append("]").toString());
        });
    }

    @PreDestroy
    public void close() {
        observableTaskRegistry.close();
        observableFileMetadataRegistry.close();
    }
}
