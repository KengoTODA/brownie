package jp.skypencil.brownie;

import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import javax.inject.Inject;

import io.vertx.core.Future;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.file.FileSystem;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
import io.vertx.rxjava.servicediscovery.types.HttpEndpoint;
import io.vertx.servicediscovery.Record;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>A server which provides API to manage file storage.
 * It abstracts distributed file system such as AWS S3.
 * This server provides following REST API:</p>
 *
 * <ul>
 * <li>{@code GET /file/}, which returns JsonArray which represents list of {@link FileMetadata}.</li>
 * <li>{@code GET /file/:fileId}, which returns file content as response body, and {@link FileMetadata} as HTTP headers. It responds 404 if specified file does not exist.</li>
 * <li>{@code HEAD /file/:fileId}, which returns file {@link FileMetadata} as HTTP headers. It responds 404 if specified file does not exist.</li>
 * <li>{@code POST /file/}, which stores uploaded file to storage and returns {@link FileMetadata} as HTTP headers.</li>
 * <li>{@code DELETE /file/:fileId}, which deletes stored file in storage. It responds 404 if specified file does not exist.</li>
 * </ul>
 *
 * <p>File storage does not support update operation, so this server provides no {@code PATCH} method nor {@code PUT} method.</p>
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

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    private final Vertx vertx;

    private final FileTransporter fileTransporter;

    private final IdGenerator idGenerator;

    private final ServiceDiscovery discovery;

    private HttpServer server;

    private String registration;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        log.info("Initializing FrontStorageServer...");

        Router router = createRouter();
        String httpHost = config().getString("BROWNIE_CLUSTER_HTTP_HOST", "localhost");
        Integer httpPort = config().getInteger("BROWNIE_CLUSTER_HTTP_PORT", 8080);
        server = vertx.createHttpServer().requestHandler(router::accept);
        server.listenObservable(httpPort)
            .flatMap(v -> {
                Record record = HttpEndpoint.createRecord("file-storage", httpHost, httpPort, "/file");
                return discovery.publishObservable(record);
            })
            .map(record -> {
                registration = record.getRegistration();
                return record;
            })
            .subscribe(v -> {
                log.info("HTTP server is listening {} port", httpPort);
                startFuture.complete();
            }, startFuture::fail);
    }

    @Override
    public void stop(Future<Void> future) {
        if (server == null) {
            future.fail(new IllegalStateException("This vertical has not been started yet"));
        } else {
            discovery.unpublishObservable(registration)
                .flatMap(v -> {
                    return server.closeObservable();
                })
                .subscribe(future::complete, future::fail);
        }
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
                response.putHeader("File-Id", tuple._1.getFileId().toString());
                response.putHeader("File-Name", tuple._1.getName());
                response.sendFile(filePath); // CONTENT_LENGTH will be put by vert.x
            }, ctx::fail);
    }

    private void headFile(RoutingContext ctx) {
        String fileId = ctx.request().getParam("fileId");
        HttpServerResponse response = ctx.response();
        log.debug("Requested to return file information which has fileId {}", fileId);

        fileTransporter
            .download(UUID.fromString(fileId))
            .subscribe(tuple -> {
                response.putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json"); // type of THIS response, not file
                response.putHeader(HttpHeaders.LAST_MODIFIED.toString(), INSTANT_FORMATTER.format(tuple._1.getGenerated()));
                response.putHeader(HttpHeaders.CONTENT_LENGTH.toString(), Long.toString(tuple._1.getContentLength()));
                response.putHeader("File-Id", tuple._1.getFileId().toString());
                response.putHeader("File-Name", tuple._1.getName());
                response.end();
            }, ctx::fail);
    }

    private void postFile(RoutingContext ctx) {
        HttpServerRequest req = ctx.request().setExpectMultipart(true);
        UUID fileId = idGenerator.generateUuidV1();

        String contentType = req.getHeader(HttpHeaders.CONTENT_TYPE.toString());
        String fileName = req.getHeader("File-Name");
        File localFile = new File(TEMP_DIR, fileId.toString());

        HttpServerResponse response = ctx.response();
        FileSystem fileSystem = vertx.fileSystem();
        req.toObservable().reduce(Buffer.buffer(), Buffer::appendBuffer).toSingle().flatMap(data -> {
            return fileSystem.writeFileObservable(localFile.getAbsolutePath(), data).toSingle();
        }).flatMap(v -> {
            return fileTransporter.upload(fileId,
                fileName,
                localFile,
                MimeType.valueOf(contentType));
        }).map(metadata -> {
            response.putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json");
            response.putHeader(HttpHeaders.LAST_MODIFIED.toString(), INSTANT_FORMATTER.format(metadata.getGenerated()));
            response.putHeader(HttpHeaders.CONTENT_LENGTH.toString(), Long.toString(metadata.getContentLength()));
            response.putHeader("File-Id", fileId.toString());
            response.putHeader("File-Name", metadata.getName());
            return metadata;
        }).subscribe(json -> {
            response.end(new JsonObject().toString());
        }, ctx::fail);
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
