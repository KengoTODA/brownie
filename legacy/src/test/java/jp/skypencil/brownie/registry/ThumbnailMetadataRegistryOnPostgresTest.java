package jp.skypencil.brownie.registry;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Objects;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.asyncsql.AsyncSQLClient;
import io.vertx.rxjava.ext.asyncsql.PostgreSQLClient;
import jp.skypencil.brownie.MimeType;
import jp.skypencil.brownie.ThumbnailMetadata;

@RunWith(VertxUnitRunner.class)
public class ThumbnailMetadataRegistryOnPostgresTest {
    private Vertx vertx;
    private AsyncSQLClient client;

    @Before
    public final void setUp() {
        vertx = Vertx.vertx();
        client = PostgreSQLClient.createShared(vertx, createConfig());
    }

    @After
    public final void cleanUp(TestContext context) {
        client.close(ar -> {
            vertx.close(context.asyncAssertSuccess());
        });
    }

    @Test
    public void testStore(TestContext context) {
        ThumbnailMetadataRegistry registry = new ThumbnailMetadataRegistryOnPostgres(client);
        UUID id = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();
        ThumbnailMetadata metadata = new ThumbnailMetadata(id, videoId, MimeType.valueOf("image/jpg"), 100, 320, 240, 0);

        Async async = context.async();
        registry.store(metadata).subscribe(v -> async.complete(), context::fail);
    }

    @Test
    public void testSearchNotExistId(TestContext context) {
        ThumbnailMetadataRegistry registry = new ThumbnailMetadataRegistryOnPostgres(client);
        UUID videoId = UUID.randomUUID();

        Async async = context.async();
        registry.search(videoId).count().subscribe(count -> {
            context.assertEquals(0, count);
        }, context::fail, async::complete);
    }

    @Test
    public void testStoreAndSearch(TestContext context) {
        ThumbnailMetadataRegistry registry = new ThumbnailMetadataRegistryOnPostgres(client);
        UUID id = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();
        ThumbnailMetadata metadata = new ThumbnailMetadata(id, videoId, MimeType.valueOf("image/jpg"), 100, 320, 240, 0);

        Async async = context.async();
        registry.store(metadata).toObservable().flatMap(v -> {
            return registry.search(videoId);
        }).filter(searched -> {
            return Objects.equal(metadata, searched);
        }).toSingle().subscribe(searched -> {
            async.complete();
        }, context::fail);
    }

    private JsonObject createConfig() {
        return new JsonObject()
                .put("host", System.getProperty("db.host", "localhost"))
                .put("port", Integer.parseInt(System.getProperty("db.port", "15432")))
                .put("username", "brownie")
                .put("password", "brownie")
                .put("database", "brownie");
    }
}
