package jp.skypencil.brownie.registry;

import java.util.UUID;

import javax.inject.Inject;

import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.ResultSet;
import io.vertx.rxjava.ext.asyncsql.AsyncSQLClient;
import jp.skypencil.brownie.ThumbnailMetadata;
import lombok.RequiredArgsConstructor;
import rx.Observable;
import rx.Single;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class ThumbnailMetadataRegistryOnPostgres
        implements ThumbnailMetadataRegistry {
    private static final String SQL_TO_INSERT = "INSERT INTO thumbnail_metadata (id, video_id, mime_type, content_length, width, height, milliseconds) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_TO_SELECT = "SELECT id, video_id, mime_type, content_length, width, height, milliseconds FROM thumbnail_metadata WHERE video_id = ? ORDER BY generated DESC";

    private final AsyncSQLClient asyncSQLClient;

    @Override
    public Single<ThumbnailMetadata> store(ThumbnailMetadata metadata) {
        return asyncSQLClient.getConnectionObservable()
                .flatMap(con -> {
                    return con.updateWithParamsObservable(SQL_TO_INSERT, metadata.toJsonArray())
                        .doAfterTerminate(con::close);
                })
                .toSingle()
                .map(ur -> {
                    if (ur.getUpdated() != 1) {
                        throw new IllegalArgumentException("Failed to store given thumbnail metadata: " + metadata);
                    }
                    return metadata;
                });
    }

    @Override
    public Observable<ThumbnailMetadata> search(UUID videoId) {
        return asyncSQLClient.getConnectionObservable()
            .flatMap(con -> {
                return con.queryWithParamsObservable(SQL_TO_SELECT, new JsonArray().add(videoId.toString()))
                    .doAfterTerminate(con::close);
            })
            .flatMapIterable(ResultSet::getRows)
            .map(ThumbnailMetadata::valueOf);
    }

}
