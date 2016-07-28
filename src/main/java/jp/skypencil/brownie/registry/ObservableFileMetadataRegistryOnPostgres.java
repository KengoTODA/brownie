package jp.skypencil.brownie.registry;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.WillNotClose;

import org.springframework.util.MimeType;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.asyncsql.AsyncSQLClient;
import io.vertx.rxjava.ext.asyncsql.PostgreSQLClient;
import io.vertx.rxjava.ext.sql.SQLConnection;
import jp.skypencil.brownie.FileId;
import jp.skypencil.brownie.FileMetadata;
import rx.Observable;

public class ObservableFileMetadataRegistryOnPostgres
        implements ObservableFileMetadataRegistry {
    private final AsyncSQLClient postgreSQLClient;

    public ObservableFileMetadataRegistryOnPostgres(Vertx vertx, JsonObject config) {
        this.postgreSQLClient = PostgreSQLClient.createShared(vertx, config);
    }

    @Override
    public Observable<FileMetadata> iterate() {
        return postgreSQLClient.getConnectionObservable()
        .flatMap(con -> {
            return con.queryObservable("SELECT id, name, mime_type, content_length, generated from file_metadata")
                    .doAfterTerminate(con::close);
        }).flatMap(rs -> {
            List<FileMetadata> iterable = rs.getResults().stream().map(json -> {
                @FileId
                UUID fileId = UUID.fromString(json.getString(0));
                MimeType mimeType = MimeType.valueOf(json.getString(2));
                Instant generated = Instant.parse(json.getString(4) + "Z");
                return new FileMetadata(fileId, json.getString(1), mimeType, json.getLong(3), generated);
            }).collect(Collectors.toList());
            return Observable.from(iterable);
        });
    }

    @Override
    public Observable<Void> store(FileMetadata metadata) {
        return postgreSQLClient.getConnectionObservable()
                .flatMap(con -> {
                    return storeInternal(con, metadata)
                            .doAfterTerminate(con::close);
                });
    }

    private Observable<Void> storeInternal(@WillNotClose SQLConnection con, FileMetadata metadata) {
        return con.setAutoCommitObservable(true)
                .flatMap(v -> {
                    return con.queryWithParamsObservable(
                            "SELECT 1 FROM file_metadata where id = ?",
                            new JsonArray().add(metadata.getFileId()));
                })
                .flatMap(rs -> {
                    if (rs.getResults().isEmpty()) {
                        JsonArray params = new JsonArray()
                                .add(metadata.getFileId())
                                .add(metadata.getName())
                                .add(metadata.getMimeType())
                                .add(metadata.getContentLength())
                                .add(metadata.getGenerated());
                        return con.updateWithParamsObservable("INSERT INTO file_metadata (id, name, mime_type, content_length, generated) VALUES (?, ?, ?, ?, ?)", params);
                    } else {
                        return Observable.error(
                                new IllegalArgumentException("FileMetadata already exists: fileId = " + metadata.getFileId()));
                    }
                }).ofType(Void.class);
    }

    @Override
    public Observable<Void> update(FileMetadata metadata) {
        return postgreSQLClient.getConnectionObservable()
            .flatMap(con -> {
                JsonArray params = new JsonArray()
                        .add(metadata.getName())
                        .add(metadata.getMimeType())
                        .add(metadata.getContentLength())
                        .add(metadata.getGenerated())
                        .add(metadata.getFileId());
                return con.updateWithParamsObservable("UPDATE file_metadata SET name = ?, mime_type = ?, content_length = ?, generated = ? WHERE id = ?", params)
                        .doAfterTerminate(con::close);
            }).ofType(Void.class);
    }

    @Override
    public Observable<FileMetadata> load(UUID fileId) {
        return postgreSQLClient.getConnectionObservable()
                .flatMap(con -> {
                    return con.queryWithParamsObservable("SELECT name, mime_type, content_length, generated FROM file_metadata WHERE id = ?",
                            new JsonArray().add(fileId.toString()))
                            .doAfterTerminate(con::close);
                }).map(rs -> {
                    List<JsonArray> result = rs.getResults();
                    if (result.isEmpty()) {
                        throw new IllegalArgumentException("FileMetadata not found: fileId = " + fileId);
                    } else {
                        JsonArray jsonObject = result.get(0);
                        Instant generated = Instant.parse(jsonObject.getString(3) + "Z");
                        return new FileMetadata(fileId,
                                jsonObject.getString(0),
                                MimeType.valueOf(jsonObject.getString(1)),
                                jsonObject.getLong(2),
                                generated);
                    }
                });
    }

    @Override
    public Observable<Void> delete(UUID fileId) {
        return postgreSQLClient.getConnectionObservable()
                .flatMap(con -> {
                    return con.updateWithParamsObservable("DELETE FROM file_metadata WHERE id = ?", new JsonArray().add(fileId.toString()))
                    .doAfterTerminate(con::close);
                }).map(rs -> {
                    if (rs.getUpdated() == 0) {
                        throw new IllegalArgumentException("FileMetadata not found: fileId = " + fileId);
                    }
                    return null;
                });
    }

    @Override
    public void close() {
        postgreSQLClient.close();
    }

}
