package jp.skypencil.brownie.registry;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.asyncsql.PostgreSQLClient;
import io.vertx.ext.sql.SQLConnection;
import jp.skypencil.brownie.Task;

public class TaskRegistryOnPostgres implements TaskRegistry {
    private final AsyncSQLClient postgreSQLClient;

    public TaskRegistryOnPostgres(Vertx vertx, JsonObject config) {
        this.postgreSQLClient = PostgreSQLClient.createShared(vertx, config);
    }

    @Override
    public void iterate(Handler<AsyncResult<ReadStream<Task>>> handler) {
        Objects.requireNonNull(handler);
        Future<ReadStream<Task>> future = Future.future();
        postgreSQLClient.getConnection(connected -> {
            if (connected.failed()) {
                future.fail(connected.cause());
                handler.handle(future);
                return;
            }
            SQLConnection con = connected.result();
            con.query("SELECT id, uploaded_file_name, resolutions, generated FROM task", selected -> {
                try (SQLConnection connection = con) {
                    if (selected.failed()) {
                        future.fail(selected.cause());
                    } else {
                        Iterator<Task> iterator = selected.result().getResults().stream().map(Task::from).iterator();
                        future.complete(new TaskReadStream(iterator));
                    }
                    handler.handle(future);
                }
            });
        });
    }

    @Override
    public void store(Task task, Handler<AsyncResult<Void>> givenHandler) {
        Future<Void> future = Future.future();
        final Handler<AsyncResult<Void>> handler =
                firstNonNull(givenHandler, result -> {});
        postgreSQLClient.getConnection(connected -> {
            if (connected.failed()) {
                future.fail(connected.cause());
                handler.handle(future);
            } else {
                SQLConnection con = connected.result();
                con.queryWithParams("INSERT INTO task (id, uploaded_file_name, resolutions, generated) VALUES (?, ?, ?, ?)", task.toJsonArray(), inserted -> {
                    try (SQLConnection connection = con) {
                        if (inserted.failed()) {
                            future.fail(inserted.cause());
                        } else {
                            future.complete();
                        }
                        handler.handle(future);
                    }
                });
            }
        });
    }

    @Override
    public void load(UUID taskId, Handler<AsyncResult<Optional<Task>>> givenHandler) {
        Future<Optional<Task>> future = Future.future();
        final Handler<AsyncResult<Optional<Task>>> handler =
                firstNonNull(givenHandler, result -> {});
        postgreSQLClient.getConnection(connected -> {
            if (connected.failed()) {
                future.fail(connected.cause());
                handler.handle(future);
            } else {
                SQLConnection con = connected.result();
                JsonArray params = new JsonArray().add(taskId.toString());
                con.queryWithParams("SELECT uploaded_file_name, resolutions, generated FROM task WHERE id = ?", params, selected -> {
                    try (SQLConnection connection = con) {
                        if (selected.failed()) {
                            future.fail(selected.cause());
                        } else {
                            Optional<Task> result = selected.result().getResults().stream().findFirst().map(array -> {
                                return Task.from(taskId, array);
                            });
                            future.complete(result);
                        }
                    }
                    handler.handle(future);
                });
            }
        });
    }

    @Nonnull
    private <T> Handler<T> firstNonNull(@Nullable Handler<T> first,
            @Nonnull Handler<T> second) {
        if (first == null) {
            return Objects.requireNonNull(second);
        } else {
            return first;
        }
    }
}
