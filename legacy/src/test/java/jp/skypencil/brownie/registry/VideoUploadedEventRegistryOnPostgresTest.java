package jp.skypencil.brownie.registry;

import java.time.Instant;
import java.util.Collections;
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
import jp.skypencil.brownie.event.VideoUploadedEvent;
import rx.Observable;

@RunWith(VertxUnitRunner.class)
public class VideoUploadedEventRegistryOnPostgresTest {
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
        Async async = context.async();

        VideoUploadedEventRegistryOnPostgres registry = new VideoUploadedEventRegistryOnPostgres(client);

        UUID taskId = UUID.randomUUID();
        VideoUploadedEvent task = new VideoUploadedEvent(taskId, "name", Collections.singleton("vga"), Instant.now());
        registry.store(task).flatMap(v -> {
            return registry.load(taskId);
        })
        .subscribe(loaded -> {
            context.assertEquals(loaded, task);
            async.complete();
        });
    }

    @Test
    public void testIterate(TestContext context) {
        Async async = context.async();

        VideoUploadedEventRegistryOnPostgres registry = new VideoUploadedEventRegistryOnPostgres(client);

        VideoUploadedEvent task1 = new VideoUploadedEvent(UUID.randomUUID(), "task1", Collections.singleton("vga"), Instant.now());
        VideoUploadedEvent task2 = new VideoUploadedEvent(UUID.randomUUID(), "task2", Collections.singleton("vga"), Instant.now());
        Observable.concat(
                registry.store(task1).toObservable(),
                registry.store(task2).toObservable())
        .all(v -> true)
        .flatMap(b -> {
            return registry.iterate().filter(task -> {
                return task.equals(task1) || task.equals(task2);
            });
        }).reduce(0, (prev, task) -> {
            return prev + 1;
        }).subscribe(sum -> {
            context.assertEquals(sum, 2);
            async.complete();
        });
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