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

import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpHeaders;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpClientResponse;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.core.streams.Pump;
import io.vertx.rxjava.ext.web.FileUpload;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.StaticHandler;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
import jp.skypencil.brownie.event.VideoUploadedEvent;
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
            File file = new File(fileUpload.uploadedFileName());
            MimeType mimeType = MimeType.valueOf(fileUpload.contentType());
            Future<Void> closed = Future.future();
            return createHttpClientForFileStorage(closed)
                .flatMap(client -> {
                    HttpClientRequest req = client.post("/file/")
                            .setChunked(true)
                            .putHeader("Content-Length", Long.toString(fileUpload.size()))
                            .putHeader(HttpHeaders.CONTENT_TYPE.toString(), mimeType.toString())
                            .putHeader("File-Name", fileUpload.fileName());
                    Single<HttpClientResponse> result = req.toObservable().toSingle();
                    vertx.fileSystem().openObservable(file.getAbsolutePath(), new OpenOptions().setRead(true).setCreateNew(false))
                        .subscribe(asyncFile -> {
                            asyncFile.endHandler(v -> {
                                req.end();
                            });
                            Pump.pump(asyncFile, req).start();
                        });
                    return result;
                })
                .flatMap(res -> {
                    if (res.statusCode() != 200) {
                        throw new RuntimeException("Failed to store uploaded video to file storage. Status code is: " + res.statusCode() + ", status message is: " + res.statusMessage());
                    }
                    UUID fileId = UUID.fromString(res.getHeader("File-Id"));
                    VideoUploadedEvent event = new VideoUploadedEvent(fileId, fileUpload.fileName(), Collections.singleton("vga"));
                    return observableTaskRegistry.store(event);
                })
                .doOnSuccess(event -> {
                    vertx.eventBus().send("file-uploaded", event);
                })
                .toObservable()
                .doOnCompleted(closed::complete);
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
        // TODO ask thumbnail service to respond thumbnail file
        throw new UnsupportedOperationException();
    }

    /**
     * A helper method to generates {@link HttpClient} to connect to file storage service with help from service discovery.
     * Caller should provide a {@link Future} and 
     * @param closed
     *      Caller should invoke {@link Future#complete()} on this instance when it finishes using created
     *      {@link HttpClient}, then this method will release reference from service discovery.
     * @return A {@link Single} which emits created {@link HttpClient}. Non-null.
     */
    @Nonnull
    private Single<HttpClient> createHttpClientForFileStorage(Future<Void> closed) {
        Single<HttpClient> clientSingle = discovery.getRecordObservable(r -> r.getName().equals("file-storage"))
            .map(discovery::getReference)
            .flatMap(reference -> {
                HttpClient client = new HttpClient(reference.get());
                closed.setHandler(ar -> {
                    // this will invoke HttpEndpointReference#close() which closes HttpClient,
                    // so we do not have to call client.close() explicitly.
                    reference.release();
                });
                return Observable.just(client);
            })
            .toSingle();
        return clientSingle;
    }

    private void deleteFile(RoutingContext ctx, String fileId) {
        io.vertx.rxjava.core.http.HttpServerResponse response = ctx.response();
        Future<Void> closed = Future.future();
        createHttpClientForFileStorage(closed)
        .flatMap(client -> {
            HttpClientRequest req = client.delete("/file/" + fileId);
            Observable<HttpClientResponse> result = req.toObservable();
            req.end();
            return result.toSingle();
        })
        .doAfterTerminate(closed::complete)
        .subscribe(clientRes -> {
            if (clientRes.statusCode() != 200) {
                throw new RuntimeException("Failed to delete file in file storage. Status code is: " + clientRes.statusCode() + ", status message is: " + clientRes.statusMessage());
            }
            response.end("deleted");
        }, ctx::fail);
    }

    private void downloadFile(RoutingContext ctx, String fileId) {
        io.vertx.rxjava.core.http.HttpServerResponse response = ctx.response().setChunked(true);
        Future<Void> closed = Future.future();
        createHttpClientForFileStorage(closed).flatMap(client -> {
            return RxHelper.get(client, "/file/" + fileId).toSingle();
        })
        .map(clientRes -> {
            response
                .putHeader("File-Id", clientRes.getHeader("File-Id"))
                .putHeader("File-Name", clientRes.getHeader("File-Name"))
                .putHeader("Last-Modified", clientRes.getHeader("Last-Modified"))
                .putHeader("Content-Length", clientRes.getHeader("Content-Length"))
                .putHeader("Content-Type", clientRes.getHeader("Content-Type"));
            return clientRes;
        })
        .map(clientRes -> {
            clientRes.exceptionHandler(ctx::fail).endHandler(v -> {
                closed.complete(v);
                response.end();
            });
            return Pump.pump(clientRes, response);
        })
        .subscribe(Pump::start);
    }

    private void generateFileList(RoutingContext ctx) {
         io.vertx.rxjava.core.http.HttpServerResponse response = ctx.response()
            .setChunked(true)
            .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json");
         Future<Void> closed = Future.future();
         createHttpClientForFileStorage(closed)
             .flatMap(client -> {
                 return RxHelper.get(client, "/file/").toSingle();
             })
             .subscribe(clientRes -> {
                 clientRes.exceptionHandler(ctx::fail).endHandler(v -> {
                     closed.complete(v);
                     response.end();
                 });
                 Pump.pump(clientRes, response).start();
             }, ctx::fail);
    }
}
