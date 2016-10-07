package jp.skypencil.brownie.registry;

import java.time.Instant;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.asyncsql.AsyncSQLClient;
import io.vertx.rxjava.ext.asyncsql.PostgreSQLClient;
import jp.skypencil.brownie.BrownieFileNotFoundException;
import jp.skypencil.brownie.FileMetadata;
import jp.skypencil.brownie.MimeType;

@RunWith(VertxUnitRunner.class)
public class FileMetadataRegistryOnPostgresTest {

    private Vertx vertx;
    private AsyncSQLClient client;
    private FileMetadataRegistryOnPostgres registry;

    @Before
    public final void setUp() {
        vertx = Vertx.vertx();
        client = PostgreSQLClient.createShared(vertx, createConfig());
        registry = new FileMetadataRegistryOnPostgres(client);
    }

    @After
    public final void cleanUp(TestContext context) {
        client.close(ar -> {
            vertx.close(context.asyncAssertSuccess());
        });
    }

    @Test
    public void testStore(TestContext context) {
        UUID fileId = UUID.randomUUID();
        Async async = context.async(2);
        FileMetadata metadata = new FileMetadata(fileId, "name", MimeType.valueOf("text/plain"), 6, Instant.now());
        registry.store(metadata).flatMap(v -> {
            return registry.load(fileId);
        }).subscribe(loaded -> {
            context.assertEquals(metadata, loaded);
            async.complete();
        }, context::fail);
    }

    @Test
    public void testOverwrite(TestContext context) {
        UUID fileId = UUID.randomUUID();
        Async async = context.async();
        FileMetadata metadata = new FileMetadata(fileId, "name", MimeType.valueOf("text/plain"), 6, Instant.now());
        registry.store(metadata).flatMap(v -> {
            return registry.store(metadata);
        }).subscribe(loaded -> {
            context.fail();
        }, error -> {
            context.assertTrue(error instanceof IllegalArgumentException);
            async.complete();
        });
    }

    @Test
    public void testLoad(TestContext context) {
        UUID fileId = UUID.randomUUID();
        Async async = context.async();
        FileMetadata metadata = new FileMetadata(fileId, "name", MimeType.valueOf("text/plain"), 6, Instant.now());
        registry.store(metadata).flatMap(v -> {
            return registry.load(fileId);
        }).subscribe(loaded -> {
            context.assertEquals(metadata, loaded);
            async.complete();
        }, context::fail);
    }

    @Test
    public void testLoadWhichDoesNotExist(TestContext context) {
        UUID fileId = UUID.randomUUID();
        Async async = context.async();
        registry.load(fileId).subscribe(loaded -> {
            context.fail();
        }, error -> {
            context.assertTrue(error instanceof BrownieFileNotFoundException);
            async.complete();
        });
    }

    @Test
    public void testUpdate(TestContext context) {
        UUID fileId = UUID.randomUUID();
        Async async = context.async(2);
        FileMetadata metadata = new FileMetadata(fileId, "name", MimeType.valueOf("text/plain"), 6, Instant.now());
        registry.store(metadata).flatMap(v -> {
            return registry.update(new FileMetadata(fileId, "updated", MimeType.valueOf("text/plain"), 6, Instant.now()));
        }).flatMap(updated -> {
            return registry.load(fileId);
        }).subscribe(loaded -> {
            context.assertNotEquals(metadata, loaded);
            context.assertEquals(loaded.getName(), "updated");
            async.complete();
        }, context::fail);
    }

    @Test
    public void testDelete(TestContext context) {
        UUID fileId = UUID.randomUUID();
        Async async = context.async();
        FileMetadata metadata = new FileMetadata(fileId, "name", MimeType.valueOf("text/plain"), 6, Instant.now());
        registry.store(metadata).flatMap(v -> {
            return registry.delete(fileId);
        }).flatMap(v -> {
            return registry.load(fileId);
        }).subscribe(v -> {
            context.fail();
        }, error -> {
            context.assertTrue(error instanceof BrownieFileNotFoundException);
            async.complete();
        });
    }

    @Test
    public void testDeleteWhichDoesNotExist(TestContext context) {
        UUID fileId = UUID.randomUUID();
        Async async = context.async();
        registry.delete(fileId).subscribe(v -> {
            context.fail();
        }, error -> {
            context.assertTrue(error instanceof BrownieFileNotFoundException);
            async.complete();
        });
    }

    @Test
    public void testIterate(TestContext context) {
        UUID fileId = UUID.randomUUID();
        Async async = context.async(2);
        FileMetadata metadata = new FileMetadata(fileId, "name", MimeType.valueOf("text/plain"), 6, Instant.now());
        registry.store(metadata).toObservable().flatMap(v -> {
            return registry.iterate();
        }).filter(iterated -> {
            return iterated.equals(metadata);
        }).count().subscribe(count -> {
            context.assertEquals(1, count);
            async.complete();
        }, context::fail, async::complete);
    }

    private JsonObject createConfig() {
        return new JsonObject()
                .put("host", System.getProperty("db.host", "localhost"))
                .put("port", Integer.parseInt(System.getProperty("db.port", "5432")))
                .put("username", "brownie")
                .put("password", "brownie")
                .put("database", "brownie");
    }
}
