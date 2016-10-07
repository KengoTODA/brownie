package jp.skypencil.brownie.registry;

import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.vertx.core.json.JsonArray;
import io.vertx.rxjava.ext.asyncsql.AsyncSQLClient;
import jp.skypencil.brownie.event.VideoUploadedEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import rx.Observable;
import rx.Single;

@RequiredArgsConstructor(
        onConstructor = @__(@Inject),
        access = AccessLevel.PACKAGE) // only for test
public class VideoUploadedEventRegistryOnPostgres implements VideoUploadedEventRegistry {
    private final AsyncSQLClient postgreSQLClient;

    @Override
    public Observable<VideoUploadedEvent> iterate() {
        return postgreSQLClient.getConnectionObservable().flatMap(con -> {
            return con.queryObservable(
                    "SELECT id, uploaded_file_name, resolutions, generated FROM video_uploaded_event ORDER BY generated DESC")
                    .doAfterTerminate(con::close);
        }).flatMap(selected -> {
            Iterable<VideoUploadedEvent> iterable = selected.getResults().stream()
                    .map(VideoUploadedEvent::from).collect(Collectors.toList());
            return Observable.from(iterable);
        });
    }

    @Override
    public Single<VideoUploadedEvent> store(VideoUploadedEvent event) {
        return postgreSQLClient.getConnectionObservable()
            .flatMap(con -> {
                return con.updateWithParamsObservable("INSERT INTO video_uploaded_event (id, uploaded_file_name, resolutions, generated) VALUES (?, ?, ?, ?)", event.toJsonArray())
                        .doAfterTerminate(con::close);
            })
            .toSingle()
            .map(ur -> {
                if (ur.getUpdated() != 1) {
                    throw new IllegalArgumentException("Failed to store given video uploaded event: " + event);
                }
                return event;
            });
    }

    @Override
    public Single<VideoUploadedEvent> load(UUID id) {
        return postgreSQLClient.getConnectionObservable()
            .flatMap(con -> {
                JsonArray params = new JsonArray().add(id.toString());
                return con.queryWithParamsObservable("SELECT uploaded_file_name, resolutions, generated FROM video_uploaded_event WHERE id = ?", params)
                        .doAfterTerminate(con::close);
            }).map(selected -> {
                if (selected.getResults().size() == 1) {
                    return VideoUploadedEvent.from(id, selected.getResults().get(0));
                }
                throw new IllegalArgumentException("Event not found with given ID: " + id);
            }).toSingle();
    }
}
