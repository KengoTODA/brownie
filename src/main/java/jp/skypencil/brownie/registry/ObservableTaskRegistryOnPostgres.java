package jp.skypencil.brownie.registry;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.asyncsql.AsyncSQLClient;
import io.vertx.rxjava.ext.asyncsql.PostgreSQLClient;
import jp.skypencil.brownie.Task;
import rx.Observable;

public class ObservableTaskRegistryOnPostgres implements ObservableTaskRegistry {
    private final AsyncSQLClient postgreSQLClient;

    public ObservableTaskRegistryOnPostgres(Vertx vertx, JsonObject config) {
        this.postgreSQLClient = PostgreSQLClient.createShared(vertx, config);
    }

    @Override
    public Observable<Task> iterate() {
        return postgreSQLClient.getConnectionObservable().flatMap(con -> {
            return con.queryObservable(
                    "SELECT id, uploaded_file_name, resolutions, generated FROM task")
                    .doAfterTerminate(con::close);
        }).flatMap(selected -> {
            Iterable<Task> iterable = selected.getResults().stream()
                    .map(json -> {
                        UUID id = UUID.fromString(json.getString(0));
                        json.remove(0);
                        return taskFrom(id, json);
                    }).collect(Collectors.toList());
            return Observable.from(iterable);
        });
    }

    @Override
    public Observable<Object> store(Task task) {
        return postgreSQLClient.getConnectionObservable()
            .flatMap(con -> {
                JsonArray params = new JsonArray()
                        .add(task.getKey().toString())
                        .add(task.getUploadedFileName())
                        .add(task.getResolutions().stream().collect(Collectors.joining(",")))
                        .add(task.getRegistered());
                return con.queryWithParamsObservable("INSERT INTO task (id, uploaded_file_name, resolutions, generated) VALUES (?, ?, ?, ?)", params)
                        .doAfterTerminate(con::close);
            });
    }

    @Override
    public Observable<Optional<Task>> load(UUID taskId) {
        return postgreSQLClient.getConnectionObservable()
            .flatMap(con -> {
                JsonArray params = new JsonArray().add(taskId.toString());
                return con.queryWithParamsObservable("SELECT uploaded_file_name, resolutions, generated FROM task WHERE id = ?", params)
                        .doAfterTerminate(con::close);
            }).map(selected -> {
                final Optional<Task> result;
                if (selected.getResults().size() == 1) {
                    result = Optional.of(taskFrom(taskId, selected.getResults().get(0)));
                } else {
                    result = Optional.empty();
                }
                return result;
            });
    }

    private Task taskFrom(UUID taskId, JsonArray json) {
        String name = json.getString(0);
        Set<String> resolutions = new HashSet<>(Arrays.asList(json.getString(1).split(",")));
        Instant generated = Instant.parse(json.getString(2) + "Z");
        return new Task(taskId, name, resolutions, generated);
    }

    @Override
    public void close() {
        postgreSQLClient.close();
    }
}
