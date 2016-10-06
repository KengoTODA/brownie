package jp.skypencil.brownie;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import com.google.common.io.Files;

import io.vertx.core.Context;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpClientResponse;
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
    public void setUp() {
        vertx = Vertx.vertx();
        fileTransporter = mock(FileTransporter.class);
        server = new FileStorageServer(vertx, fileTransporter, new IdGenerator());

        Context context = mock(Context.class);
        doReturn(new JsonObject().put("BROWNIE_CLUSTER_HTTP_PORT", HTTP_PORT)).when(context).config();

        server.init((io.vertx.core.Vertx) vertx.getDelegate(), context);
        server.start();
    }

    @After
    public void cleanUp(TestContext context) throws Exception {
        server.stop();
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testListWhenWeHaveNoFileInStorage(TestContext context) {
        Async async = context.async();
        doReturn(Observable.empty()).when(fileTransporter).list();

        server.getListenedFuture().subscribe(v -> {
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
        }, context::fail);
    }

    @Test
    public void testGet(TestContext context) throws IOException {
        Async async = context.async();
        UUID fileId = UUID.randomUUID();
        File file = folder.newFile();
        Files.write("file", file, StandardCharsets.UTF_8);

        FileMetadata metadata = new FileMetadata(fileId, "name", MimeType.valueOf("text/plain"), 4, Instant.parse("2007-12-03T10:15:30.00Z"));
        doReturn(Single.just(Tuple2.apply(metadata, file))).when(fileTransporter).download(fileId);

        server.getListenedFuture().subscribe(v -> {
            HttpClientRequest req = vertx.createHttpClient().get(HTTP_PORT, "localhost", "/file/" + fileId);
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
                        context.assertEquals("file", buffer.toString());
                        async.complete();
                    }, context::fail);
            req.end();
        }, context::fail);
    }

    @Test
    public void testGetNotExistFile(TestContext context) throws IOException {
        Async async = context.async();
        UUID fileId = UUID.randomUUID();

        doReturn(Single.error(new BrownieFileNotFoundException(fileId))).when(fileTransporter).download(fileId);

        server.getListenedFuture().subscribe(v -> {
            HttpClientRequest req = vertx.createHttpClient().get(HTTP_PORT, "localhost", "/file/" + fileId);
            req.toObservable()
                .subscribe(res -> {
                    context.assertEquals(404, res.statusCode());
                    async.complete();
                }, context::fail);
            req.end();
        }, context::fail);
    }

    @Test
    public void testHead(TestContext context) throws IOException {
        Async async = context.async();
        UUID fileId = UUID.randomUUID();

        FileMetadata metadata = new FileMetadata(fileId, "name", MimeType.valueOf("text/plain"), 4, Instant.parse("2007-12-03T10:15:30.00Z"));
        doReturn(Single.just(Tuple2.apply(metadata, null))).when(fileTransporter).download(fileId);

        server.getListenedFuture().subscribe(v -> {
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
        }, context::fail);
    }

    @Test
    public void testHeadNotExistFile(TestContext context) throws IOException {
        Async async = context.async();
        UUID fileId = UUID.randomUUID();

        doReturn(Single.error(new BrownieFileNotFoundException(fileId))).when(fileTransporter).download(fileId);

        server.getListenedFuture().subscribe(v -> {
            HttpClientRequest req = vertx.createHttpClient().head(HTTP_PORT, "localhost", "/file/" + fileId);
            req.toObservable()
                .subscribe(res -> {
                    context.assertEquals(404, res.statusCode());
                    async.complete();
                }, context::fail);
            req.end();
        }, context::fail);
    }

    @Test
    public void testDelete(TestContext context) {
        Async async = context.async();
        UUID fileId = UUID.randomUUID();
        doReturn(Single.just(fileId)).when(fileTransporter).delete(fileId);

        server.getListenedFuture().subscribe(v -> {
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
        }, context::fail);
    }

    @Test
    public void testDeleteNotExistFile(TestContext context) {
        Async async = context.async();
        UUID fileId = UUID.randomUUID();
        doReturn(Single.error(new BrownieFileNotFoundException(fileId))).when(fileTransporter).delete(fileId);

        server.getListenedFuture().subscribe(v -> {
            HttpClientRequest req = vertx.createHttpClient().delete(HTTP_PORT, "localhost", "/file/" + fileId);
            req.toObservable()
                .subscribe(res -> {
                    context.assertEquals(404, res.statusCode());
                    async.complete();
                }, context::fail);
            req.end();
        }, context::fail);
    }
}
