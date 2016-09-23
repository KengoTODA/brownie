package jp.skypencil.brownie.registry;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import io.vertx.core.json.JsonArray;
import io.vertx.rxjava.ext.asyncsql.AsyncSQLClient;
import jp.skypencil.brownie.event.VideoUploadedEvent;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import rx.Observable;

@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE) // only for test
public class VideoUploadedEventRegistryOnPostgres implements VideoUploadedEventRegistry {
    @Resource
    private AsyncSQLClient postgreSQLClient;

    @Override
    public Observable<VideoUploadedEvent> iterate() {
        return postgreSQLClient.getConnectionObservable().flatMap(con -> {
            return con.queryObservable(
                    "SELECT id, uploaded_file_name, resolutions, generated FROM task")
                    .doAfterTerminate(con::close);
        }).flatMap(selected -> {
            Iterable<VideoUploadedEvent> iterable = selected.getResults().stream()
                    .map(VideoUploadedEvent::from).collect(Collectors.toList());
            return Observable.from(iterable);
        });
    }

    @Override
    public Observable<Object> store(VideoUploadedEvent task) {
        return postgreSQLClient.getConnectionObservable()
            .flatMap(con -> {
                return con.queryWithParamsObservable("INSERT INTO task (id, uploaded_file_name, resolutions, generated) VALUES (?, ?, ?, ?)", task.toJsonArray())
                        .doAfterTerminate(con::close);
            });
    }

    @Override
    public Observable<Optional<VideoUploadedEvent>> load(UUID taskId) {
        return postgreSQLClient.getConnectionObservable()
            .flatMap(con -> {
                JsonArray params = new JsonArray().add(taskId.toString());
                return con.queryWithParamsObservable("SELECT uploaded_file_name, resolutions, generated FROM task WHERE id = ?", params)
                        .doAfterTerminate(con::close);
            }).map(selected -> {
                final Optional<VideoUploadedEvent> result;
                if (selected.getResults().size() == 1) {
                    result = Optional.of(VideoUploadedEvent.from(taskId, selected.getResults().get(0)));
                } else {
                    result = Optional.empty();
                }
                return result;
            });
    }
}
