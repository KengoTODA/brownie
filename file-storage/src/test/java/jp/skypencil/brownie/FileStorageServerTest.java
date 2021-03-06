package jp.skypencil.brownie;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpClientResponse;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
import rx.Observable;
import rx.Single;
import scala.Tuple2;

@RunWith(VertxUnitRunner.class)
public class FileStorageServerTest {
    private static final int HTTP_PORT = 18080;

    private Vertx vertx;
    private FileStorageServer server;
    private FileTransporter fileTransporter;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();
        fileTransporter = mock(FileTransporter.class);
        server = new FileStorageServer(vertx, fileTransporter, new IdGenerator(), ServiceDiscovery.create(vertx));

        Context ctx = mock(Context.class);
        doReturn(new JsonObject().put("BROWNIE_CLUSTER_HTTP_PORT", HTTP_PORT)).when(ctx).config();

        server.init((io.vertx.core.Vertx) vertx.getDelegate(), ctx);
        Future<Void> future = Future.future();
        future.setHandler(context.asyncAssertSuccess());
        server.start(future);
    }

    @After
    public void cleanUp(TestContext context) {
        Future<Void> stopped = Future.future();
        Handler<AsyncResult<Void>> handler = context.asyncAssertSuccess();
        stopped.setHandler(ar -> {
            context.assertFalse(ar.failed());
            vertx.close(handler);
        });
        server.stop(stopped);
    }

    @Test
    public void testListWhenWeHaveNoFileInStorage(TestContext context) {
        Async async = context.async();
        doReturn(Observable.empty()).when(fileTransporter).list();

        HttpClientRequest req = vertx.createHttpClient().get(HTTP_PORT, "localhost", "/file");
        req.toObservable()
                .flatMap(HttpClientResponse::toObservable)
                .reduce(Buffer.buffer(), Buffer::appendBuffer)
                .toSingle()
                .subscribe(buffer -> {
                    context.assertEquals(new JsonArray(), buffer.toJsonArray());
                    async.complete();
                }, context::fail);
        req.end();
    }

    @Test
    public void testGet(TestContext context) throws IOException {
        testGetInternal(context, "1KiB.dat", 1024);
    }

    @Test
    public void testGet1MiB(TestContext context) throws IOException {
        testGetInternal(context, "1MiB.dat", 1024 * 1024);
    }

    private void testGetInternal(TestContext context, String fileName, int fileSize) throws IOException {
        Async async = context.async();
        UUID fileId = UUID.randomUUID();
        File file = new File("src/test/resources/", fileName);

        FileMetadata metadata = new FileMetadata(fileId, fileName, MimeType.valueOf("application/octet-stream"),
                fileSize, Instant.parse("2007-12-03T10:15:30.00Z"));
        doReturn(Single.just(Tuple2.apply(metadata, file))).when(fileTransporter).download(fileId);

        HttpClient client = vertx.createHttpClient();
        RxHelper.get(client, HTTP_PORT, "localhost", "/file/" + fileId)
            .flatMap(res -> {
                context.assertEquals(
                        Integer.toString(fileSize),
                        res.getHeader(HttpHeaders.CONTENT_LENGTH.toString()));
                context.assertEquals(
                        "Mon, 03 12 2007 10:15:30 GMT",
                        res.getHeader(HttpHeaders.LAST_MODIFIED.toString()));
                return res.toObservable();
            }).reduce(0, (size, buffer) -> {
                return size + buffer.length();
            }).subscribe(downloadedBytes -> {
                context.assertEquals(fileSize, downloadedBytes.intValue());
            }, context::fail, () -> {
                client.close();
                async.complete();
            });
    }
    @Test
    public void testGetNotExistFile(TestContext context) throws IOException {
        Async async = context.async();
        UUID fileId = UUID.randomUUID();

        doReturn(Single.error(new BrownieFileNotFoundException(fileId))).when(fileTransporter).download(fileId);

        HttpClientRequest req = vertx.createHttpClient().get(HTTP_PORT, "localhost", "/file/" + fileId);
        req.toObservable()
            .subscribe(res -> {
                context.assertEquals(404, res.statusCode());
                async.complete();
            }, context::fail);
        req.end();
    }

    @Test
    public void testHead(TestContext context) throws IOException {
        Async async = context.async();
        UUID fileId = UUID.randomUUID();

        FileMetadata metadata = new FileMetadata(fileId, "name", MimeType.valueOf("text/plain"), 4, Instant.parse("2007-12-03T10:15:30.00Z"));
        doReturn(Single.just(Tuple2.apply(metadata, null))).when(fileTransporter).download(fileId);

        HttpClientRequest req = vertx.createHttpClient().head(HTTP_PORT, "localhost", "/file/" + fileId);
        req.toObservable()
                .map(res -> {
                    context.assertEquals(
                            "Mon, 03 12 2007 10:15:30 GMT",
                            res.getHeader(HttpHeaders.LAST_MODIFIED.toString()));
                    return res;
                })
                .flatMap(HttpClientResponse::toObservable)
                .reduce(Buffer.buffer(), Buffer::appendBuffer)
                .toSingle()
                .subscribe(buffer -> {
                    context.assertEquals("", buffer.toString());
                    async.complete();
                }, context::fail);
        req.end();
    }

    @Test
    public void testHeadNotExistFile(TestContext context) throws IOException {
        Async async = context.async();
        UUID fileId = UUID.randomUUID();

        doReturn(Single.error(new BrownieFileNotFoundException(fileId))).when(fileTransporter).download(fileId);

        HttpClientRequest req = vertx.createHttpClient().head(HTTP_PORT, "localhost", "/file/" + fileId);
        req.toObservable()
            .subscribe(res -> {
                context.assertEquals(404, res.statusCode());
                async.complete();
            }, context::fail);
        req.end();
    }

    @Test
    public void testDelete(TestContext context) {
        Async async = context.async();
        UUID fileId = UUID.randomUUID();
        doReturn(Single.just(fileId)).when(fileTransporter).delete(fileId);

        HttpClientRequest req = vertx.createHttpClient().delete(HTTP_PORT, "localhost", "/file/" + fileId);
        req.toObservable()
                .flatMap(HttpClientResponse::toObservable)
                .reduce(Buffer.buffer(), Buffer::appendBuffer)
                .toSingle()
                .subscribe(buffer -> {
                    context.assertEquals(new JsonObject().put("id", fileId.toString()), buffer.toJsonObject());
                    async.complete();
                }, context::fail);
        req.end();
    }

    @Test
    public void testDeleteNotExistFile(TestContext context) {
        Async async = context.async();
        UUID fileId = UUID.randomUUID();
        doReturn(Single.error(new BrownieFileNotFoundException(fileId))).when(fileTransporter).delete(fileId);

        HttpClientRequest req = vertx.createHttpClient().delete(HTTP_PORT, "localhost", "/file/" + fileId);
        req.toObservable()
            .subscribe(res -> {
                context.assertEquals(404, res.statusCode());
                async.complete();
            }, context::fail);
        req.end();
    }

    @Test
    public void testPost(TestContext context) throws IOException {
        Async async = context.async();
        UUID fileId = UUID.randomUUID();
        File file = folder.newFile();
        Files.write(file.toPath(), "file".getBytes(StandardCharsets.UTF_8));
        MimeType mimetype = MimeType.valueOf("text/plain");
        FileMetadata metadata = new FileMetadata(fileId, "name", mimetype, 4, Instant.now());
        doReturn(Single.just(metadata)).when(fileTransporter)
                .upload(any(UUID.class), eq(metadata.getName()), any(File.class), eq(mimetype));

        HttpClientRequest req = vertx.createHttpClient().post(HTTP_PORT, "localhost", "/file/")
                .setChunked(true)
                .putHeader(HttpHeaders.CONTENT_TYPE.toString(), mimetype.toString())
                .putHeader("File-Name", metadata.getName());
        req.toObservable()
                .toSingle()
                .subscribe(res -> {
                    context.assertNotNull(res.getHeader("File-Id"));
                    context.assertNotEquals(fileId, res.getHeader("File-Id"));
                    context.assertEquals("application/json", res.getHeader(HttpHeaders.CONTENT_TYPE.toString()));
                    context.assertEquals("name", res.getHeader("File-Name"));
                    async.complete();
                }, context::fail);
        vertx.fileSystem()
            .readFileObservable(file.getAbsolutePath())
            .subscribe(req::write, context::fail, req::end);
    }
}
