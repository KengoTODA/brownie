package jp.skypencil.brownie;

import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.vertx.core.Future;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.web.FileUpload;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.Single;

/**
 * A server which provides API to manage file storage.
 * It abstracts distributed file system such as AWS S3.
 */
@RequiredArgsConstructor(
        onConstructor = @__(@Inject),
        access = AccessLevel.PACKAGE) // for unit test
@Slf4j
class FileStorageServer extends AbstractVerticle {
    /**
     * RFC850 format, which is used by {@code Last-Modified} header
     */
    private static final String HTTP_DATE_FORMAT = "EEE, dd MM yyyy HH:mm:ss z";

    private static final DateTimeFormatter INSTANT_FORMATTER = DateTimeFormatter
            .ofPattern(HTTP_DATE_FORMAT).withLocale(Locale.ENGLISH).withZone(ZoneId.of("GMT"));

    private final Vertx vertx;

    private final FileTransporter fileTransporter;

    private final IdGenerator idGenerator;

    private HttpServer server;

    @Getter(AccessLevel.PACKAGE)
    private final ObservableFuture<HttpServer> listenedFuture = new ObservableFuture<>();

    @Override
    public void start() {
        log.info("Initializing FrontStorageServer...");

        Router router = createRouter();
        Integer httpPort = config().getInteger("BROWNIE_CLUSTER_HTTP_PORT", 8080);
        server = vertx.createHttpServer().requestHandler(router::accept).listen(httpPort, listenedFuture.toHandler());
        log.info("HTTP server is listening {} port", httpPort);
    }

    @Override
    public void stop(Future<Void> future) {
        server.close(ar -> {
            if (ar.failed()) {
                future.fail(ar.cause());
            } else {
                future.complete();
            }
        });
    }

    private Router createRouter() {
        Router router = Router.router(vertx);
        router.get("/file/").handler(this::listFile);
        router.get("/file/:fileId").handler(this::getFile).failureHandler(this::judgeStatusCode);
        router.head("/file/:fileId").handler(this::headFile).failureHandler(this::judgeStatusCode);
        router.post("/file").handler(this::postFile);
        router.delete("/file/:fileId").handler(this::deleteFile).failureHandler(this::judgeStatusCode);
        return router;
    }

    private void judgeStatusCode(RoutingContext ctx) {
        if (ctx.failure() instanceof BrownieFileNotFoundException) {
            ctx.response().setStatusCode(404).end("File not found");
        } else {
            ctx.next();
        }
    }

    private void listFile(RoutingContext ctx) {
        HttpServerResponse response = ctx.response()
                .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
                .setChunked(true)
                .write("[");
        log.debug("Requested to list files");

        // TODO consider paging
        fileTransporter.list()
            .map(FileMetadata::toJsonObject)
            .subscribe(json -> {
                if (response.bytesWritten() > 1) {
                    response.write(",");
                }
                response.write(json.toString());
            }, ctx::fail, () -> {
                response.end("]");
        });
    }

    private void getFile(RoutingContext ctx) {
        String fileId = ctx.request().getParam("fileId");
        HttpServerResponse response = ctx.response();
        log.debug("Requested to get file which has fileId {}", fileId);

        fileTransporter
            .download(UUID.fromString(fileId))
            .subscribe(tuple -> {
                String filePath = tuple._2.getAbsolutePath();
                response.putHeader(HttpHeaders.CONTENT_TYPE.toString(), tuple._1.getMimeType().toString());
                response.putHeader(HttpHeaders.LAST_MODIFIED.toString(), INSTANT_FORMATTER.format(tuple._1.getGenerated()));
                response.sendFile(filePath); // CONTENT_LENGTH will be put by vert.x
                response.putHeader("File-Id", tuple._1.getFileId().toString());
                response.putHeader("File-Name", tuple._1.getName());
            }, ctx::fail);
    }

    private void headFile(RoutingContext ctx) {
        String fileId = ctx.request().getParam("fileId");
        HttpServerResponse response = ctx.response();
        log.debug("Requested to return file information which has fileId {}", fileId);

        fileTransporter
            .download(UUID.fromString(fileId))
            .subscribe(tuple -> {
                response.putHeader(HttpHeaders.CONTENT_TYPE.toString(), tuple._1.getMimeType().toString());
                response.putHeader(HttpHeaders.LAST_MODIFIED.toString(), INSTANT_FORMATTER.format(tuple._1.getGenerated()));
                response.putHeader(HttpHeaders.CONTENT_LENGTH.toString(), Long.toString(tuple._1.getContentLength()));
                response.putHeader("File-Id", tuple._1.getFileId().toString());
                response.putHeader("File-Name", tuple._1.getName());
                response.end();
            }, ctx::fail);
    }

    private void postFile(RoutingContext ctx) {
        Set<FileUpload> uploadedFile = ctx.fileUploads();
        if (uploadedFile.isEmpty()) {
            ctx.fail(new IllegalArgumentException("Please upload file"));
        }

        Observable<FileMetadata> result = uploadedFile.stream().map(fileUpload -> {
            UUID fileId = idGenerator.generateUuidV1();
            return fileTransporter.upload(fileId,
                    fileUpload.fileName(),
                    new File(fileUpload.uploadedFileName()),
                    MimeType.valueOf(fileUpload.contentType()));
        })
        .map(Single::toObservable)
        .collect(Collectors.reducing(Observable.empty(), Observable::merge));

        HttpServerResponse response = ctx.response()
                .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
                .write("[");

        result.map(FileMetadata::toJsonObject).subscribe(json -> {
            if (response.bytesWritten() > 1) {
                response.write(",");
            }
            response.write(json.toString());
        }, ctx::fail, () -> {
            response.end("]");
        });
    }

    private void deleteFile(RoutingContext ctx) {
        String fileId = ctx.request().getParam("fileId");
        log.debug("Requested to delete file which has fileId {}", fileId);

        HttpServerResponse response = ctx.response()
                .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json");

        fileTransporter.delete(UUID.fromString(fileId)).subscribe(v -> {
            response.end(new JsonObject().put("id", fileId).toString());
        }, ctx::fail);
    }
}
