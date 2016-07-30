package jp.skypencil.brownie.registry;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import io.vertx.core.json.JsonArray;
import io.vertx.rxjava.ext.asyncsql.AsyncSQLClient;
import jp.skypencil.brownie.Task;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import rx.Observable;

@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE) // only for test
public class ObservableTaskRegistryOnPostgres implements ObservableTaskRegistry {
    @Resource
    private AsyncSQLClient postgreSQLClient;

    @Override
    public Observable<Task> iterate() {
        return postgreSQLClient.getConnectionObservable().flatMap(con -> {
            return con.queryObservable(
                    "SELECT id, uploaded_file_name, resolutions, generated FROM task")
                    .doAfterTerminate(con::close);
        }).flatMap(selected -> {
            Iterable<Task> iterable = selected.getResults().stream()
                    .map(Task::from).collect(Collectors.toList());
            return Observable.from(iterable);
        });
    }

    @Override
    public Observable<Object> store(Task task) {
        return postgreSQLClient.getConnectionObservable()
            .flatMap(con -> {
                return con.queryWithParamsObservable("INSERT INTO task (id, uploaded_file_name, resolutions, generated) VALUES (?, ?, ?, ?)", task.toJsonArray())
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
                    result = Optional.of(Task.from(taskId, selected.getResults().get(0)));
                } else {
                    result = Optional.empty();
                }
                return result;
            });
    }

    @Override
    public void close() {
        postgreSQLClient.close();
    }
}
