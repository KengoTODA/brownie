package jp.skypencil.brownie;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Context;
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

@RunWith(VertxUnitRunner.class)
public class FileStorageServerTest {
    private static final int HTTP_PORT = 18080;

    private Vertx vertx;
    private FileStorageServer server;
    private FileTransporter fileTransporter;

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
