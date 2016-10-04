package jp.skypencil.brownie.registry;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.WillNotClose;
import javax.inject.Inject;

import io.vertx.core.json.JsonArray;
import io.vertx.rxjava.ext.asyncsql.AsyncSQLClient;
import io.vertx.rxjava.ext.sql.SQLConnection;
import jp.skypencil.brownie.FileId;
import jp.skypencil.brownie.FileMetadata;
import jp.skypencil.brownie.MimeType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import rx.Observable;
import rx.Single;

@RequiredArgsConstructor(
        onConstructor = @__(@Inject),
        access = AccessLevel.PACKAGE) // only for Unit test
public class FileMetadataRegistryOnPostgres
        implements FileMetadataRegistry {
    private final AsyncSQLClient postgreSQLClient;

    @Override
    public Observable<FileMetadata> iterate() {
        return postgreSQLClient.getConnectionObservable()
        .flatMap(con -> {
            return con.queryObservable("SELECT id, name, mime_type, content_length, generated from file_metadata ORDER BY generated DESC")
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
    public Single<FileMetadata> store(FileMetadata metadata) {
        return postgreSQLClient.getConnectionObservable()
                .toSingle()
                .flatMap(con -> {
                    return storeInternal(con, metadata)
                            .doAfterTerminate(con::close);
                })
                .map(v -> metadata);
    }

    private Single<Void> storeInternal(@WillNotClose SQLConnection con, FileMetadata metadata) {
        return con.setAutoCommitObservable(false)
                .flatMap(v -> {
                    return con.queryWithParamsObservable(
                            "SELECT 1 FROM file_metadata where id = ? FOR UPDATE",
                            new JsonArray().add(metadata.getFileId().toString()));
                })
                .flatMap(rs -> {
                    if (rs.getResults().isEmpty()) {
                        JsonArray params = new JsonArray()
                                .add(metadata.getFileId().toString())
                                .add(metadata.getName())
                                .add(metadata.getMimeType().toString())
                                .add(metadata.getContentLength())
                                .add(metadata.getGenerated());
                        return con.updateWithParamsObservable("INSERT INTO file_metadata (id, name, mime_type, content_length, generated) VALUES (?, ?, ?, ?, ?)", params);
                    } else {
                        return Observable.error(
                                new IllegalArgumentException("FileMetadata already exists: fileId = " + metadata.getFileId()));
                    }
                }).flatMap(ur-> {
                    return con.commitObservable();
                }).toSingle();
    }

    @Override
    public Single<FileMetadata> update(FileMetadata metadata) {
        return postgreSQLClient.getConnectionObservable()
            .flatMap(con -> {
                JsonArray params = new JsonArray()
                        .add(metadata.getName())
                        .add(metadata.getMimeType().toString())
                        .add(metadata.getContentLength())
                        .add(metadata.getGenerated())
                        .add(metadata.getFileId().toString());
                return con.updateWithParamsObservable("UPDATE file_metadata SET name = ?, mime_type = ?, content_length = ?, generated = ? WHERE id = ?", params)
                        .doAfterTerminate(con::close);
            }).toSingle().map(ur -> {
                return metadata;
            });
    }

    @Override
    public Single<FileMetadata> load(UUID fileId) {
        return postgreSQLClient.getConnectionObservable()
                .flatMap(con -> {
                    return con.queryWithParamsObservable("SELECT name, mime_type, content_length, generated FROM file_metadata WHERE id = ?",
                            new JsonArray().add(fileId.toString()))
                            .doAfterTerminate(con::close);
                }).toSingle().map(rs -> {
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
    public Single<UUID> delete(UUID fileId) {
        return postgreSQLClient.getConnectionObservable()
                .flatMap(con -> {
                    return con.updateWithParamsObservable("DELETE FROM file_metadata WHERE id = ?", new JsonArray().add(fileId.toString()))
                    .doAfterTerminate(con::close);
                }).toSingle().map(rs -> {
                    if (rs.getUpdated() == 0) {
                        throw new IllegalArgumentException("FileMetadata not found: fileId = " + fileId);
                    }
                    return fileId;
                });
    }
}
