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
import io.vertx.rxjava.ext.asyncsql.PostgreSQLClient;
import jp.skypencil.brownie.Task;
import rx.Observable;

@RunWith(VertxUnitRunner.class)
public class ObservableTaskRegistryOnPostgresTest {
    private Vertx vertx;

    @Before
    public final void setUp() {
        vertx = Vertx.vertx();
    }

    @After
    public final void cleanUp(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testStore(TestContext context) {
        Async async = context.async();

        ObservableTaskRegistryOnPostgres registry = new ObservableTaskRegistryOnPostgres(PostgreSQLClient.createShared(vertx, createConfig()));

        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "name", Collections.singleton("vga"), Instant.now());
        registry.store(task).flatMap(v -> {
            return registry.load(taskId);
        })
        .subscribe(loaded -> {
            context.assertEquals(loaded.get(), task);
            registry.close();
            async.complete();
        });
    }

    @Test
    public void testIterate(TestContext context) {
        Async async = context.async();

        ObservableTaskRegistryOnPostgres registry = new ObservableTaskRegistryOnPostgres(PostgreSQLClient.createShared(vertx, createConfig()));

        Task task1 = new Task(UUID.randomUUID(), "task1", Collections.singleton("vga"), Instant.now());
        Task task2 = new Task(UUID.randomUUID(), "task2", Collections.singleton("vga"), Instant.now());
        Observable.concat(
                registry.store(task1),
                registry.store(task2))
        .all(v -> true)
        .flatMap(b -> {
            return registry.iterate().filter(task -> {
                return task.equals(task1) || task.equals(task2);
            });
        }).reduce(0, (prev, task) -> {
            return prev + 1;
        }).subscribe(sum -> {
            context.assertEquals(sum, 2);
            registry.close();
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
