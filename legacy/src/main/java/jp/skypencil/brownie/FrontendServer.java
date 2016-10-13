package jp.skypencil.brownie;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import io.vertx.core.http.HttpHeaders;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpClientResponse;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.web.FileUpload;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.StaticHandler;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
import jp.skypencil.brownie.event.VideoUploadedEvent;
import jp.skypencil.brownie.registry.FileMetadataRegistry;
import jp.skypencil.brownie.registry.ThumbnailMetadataRegistry;
import jp.skypencil.brownie.registry.VideoUploadedEventRegistry;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.Single;



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
@Slf4j
@RequiredArgsConstructor(
        onConstructor = @__(@Inject),
        access = AccessLevel.PACKAGE) // for unit test
public class FrontendServer extends AbstractVerticle {
    /**
     * Directory to store uploaded file.
     */
    private final String directory = createDirectory();

    private final VideoUploadedEventRegistry observableTaskRegistry;

    private final FileMetadataRegistry observableFileMetadataRegistry;

    private final ThumbnailMetadataRegistry thumbnailMetadataRegistry;

    private final IdGenerator idGenerator;

    private final ServiceDiscovery discovery;

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

    @Override
    public void start() throws Exception {
        createServer();
    }

    /**
     * Create {@link HttpServer} to listen specified {@link #config()}.
     */
    private void createServer(){
        Router router = Router.router(vertx);

        // Serve the form handling part
        router.route("/form").handler(BodyHandler.create().setUploadsDirectory(directory));
        router.post("/form").handler(this::handleForm);
        router.mountSubRouter("/tasks", createRouterForTaskApi());
        router.mountSubRouter("/files", createRouterForFileApi());
        router.mountSubRouter("/thumbnail", createRouterForThumbnailApi());
        // Serve the static pages
        router.route().handler(StaticHandler.create());

        Integer httpPort = config().getInteger("BROWNIE_CLUSTER_HTTP_PORT", 8080);
        vertx.createHttpServer().requestHandler(router::accept).listen(httpPort);
        log.info("HTTP server is listening {} port", httpPort);
    }

    void handleForm(RoutingContext ctx) {
        HttpServerResponse response = ctx.response()
                .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "text/plain");
        Set<FileUpload> uploadedFiles = ctx.fileUploads();
        if (uploadedFiles.isEmpty()) {
            response.end("No file uploaded");
            return;
        }
        Observable.from(uploadedFiles).flatMap(fileUpload -> {
            VideoUploadedEvent task = new VideoUploadedEvent(idGenerator.generateUuidV1(), fileUpload.fileName(), Collections.singleton("vga"));
            File file = new File(fileUpload.uploadedFileName());
            MimeType mimeType = MimeType.valueOf(fileUpload.contentType());
            return createHttpClientForFileStorage()
                .flatMap(client -> {
                    HttpClientRequest req = client.post("/file/")
                            .putHeader(HttpHeaders.CONTENT_TYPE.toString(), mimeType.toString())
                            .putHeader("File-Name", fileUpload.fileName());
                    vertx.fileSystem().readFileObservable(file.getAbsolutePath())
                        .subscribe(req::write, ctx::fail, req::end);
                    return req.toObservable().toSingle();
                })
                .flatMap(res -> {
                    if (res.statusCode() != 200) {
                        throw new RuntimeException("Failed to store uploaded video to file storage. Status code is: " + res.statusCode() + ", status message is: " + res.statusMessage());
                    }
                    return observableTaskRegistry.store(task);
                })
                .toObservable()
                .doOnCompleted(() -> {
                    vertx.eventBus().send("file-uploaded", task);
                });
        }).subscribe(v -> {}, error -> {
            log.warn("Failed to store task to registry", error);
            ctx.fail(error);
        }, () -> {
            response.end("registered");
        });
    }

    private Router createRouterForTaskApi() {
        Router subRouter = Router.router(vertx);
        subRouter.route().handler(ctx -> {
            HttpServerResponse response = ctx.response()
                    .setChunked(true)
                    .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json");
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

    private Router createRouterForThumbnailApi() {
        Router subRouter = Router.router(vertx);
        subRouter.get("/:videoId").handler(this::downloadThumbnail);
        return subRouter;
    }

    private void downloadThumbnail(RoutingContext ctx) {
        UUID videoId = UUID.fromString(ctx.request().getParam("videoId"));
        HttpServerResponse response = ctx.response();
        Single<HttpClient> clientSingle = createHttpClientForFileStorage();
        Single<UUID> thumbnailIdSingle = findThumbnailFileIdFor(videoId);
        Single.zip(clientSingle, thumbnailIdSingle, (client, thumbnailId) -> {
            return client.get("/file/" + thumbnailId);
        })
        .toObservable()
        .flatMap(clientReq -> {
            Observable<Buffer> result = clientReq.toObservable()
                    .flatMap(HttpClientResponse::toObservable);
            clientReq.end();
            return result;
        })
        .subscribe(response::write, ctx::fail, response::end);
    }

    private Single<HttpClient> createHttpClientForFileStorage() {
        Single<HttpClient> clientSingle = discovery.getRecordObservable(r -> r.getName().equals("file-storage"))
            .map(discovery::getReference)
            .flatMap(reference -> {
                HttpClient client = reference.get();
                return Observable.just(client)
                        .doAfterTerminate(client::close)
                        .doAfterTerminate(reference::release);
            })
            .toSingle();
        return clientSingle;
    }

    private Single<UUID> findThumbnailFileIdFor(UUID videoId) {
        return thumbnailMetadataRegistry.search(videoId)
                .first().toSingle()
                .map(ThumbnailMetadata::getId);
    }

    private void deleteFile(RoutingContext ctx, String fileId) {
        io.vertx.rxjava.core.http.HttpServerResponse response = ctx.response();
        createHttpClientForFileStorage()
        .flatMap(client -> {
            HttpClientRequest req = client.delete("/file/" + fileId);
            Observable<HttpClientResponse> result = req.toObservable();
            req.end();
            return result.toSingle();
        })
        .subscribe(clientRes -> {
            if (clientRes.statusCode() != 200) {
                throw new RuntimeException("Failed to delete file in file storage. Status code is: " + clientRes.statusCode() + ", status message is: " + clientRes.statusMessage());
            }
            response.end("deleted");
        }, ctx::fail);
    }

    private void downloadFile(RoutingContext ctx, String fileId) {
        UUID id = UUID.fromString(fileId);
        io.vertx.rxjava.core.http.HttpServerResponse response = ctx.response();
        observableFileMetadataRegistry.load(id).subscribe(metadata -> {
            long contentLength = metadata.getContentLength();
            response.putHeader("Content-Length", Long.toString(contentLength));
            createHttpClientForFileStorage().flatMap(client -> {
                HttpClientRequest clientReq = client.get("/file/" + fileId);
                Single<HttpClientResponse> result = clientReq.toObservable().toSingle();
                clientReq.end();
                return result;
            })
            .toObservable()
            .flatMap(HttpClientResponse::toObservable)
            .subscribe(response::write, ctx::fail, response::end);
        });
    }

    private void generateFileList(RoutingContext ctx) {
         io.vertx.rxjava.core.http.HttpServerResponse response = ctx.response()
            .setChunked(true)
            .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json");
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
}
