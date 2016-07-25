package jp.skypencil.brownie.registry;

import java.time.Instant;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;

import org.springframework.util.MimeType;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.asyncsql.PostgreSQLClient;
import io.vertx.ext.sql.SQLConnection;
import jp.skypencil.brownie.FileMetadata;

public class FileMetadataRegistryOnPostgres implements FileMetadataRegistry, AutoCloseable {
    private final AsyncSQLClient postgreSQLClient;

    public FileMetadataRegistryOnPostgres(String host, Vertx vertx) {
        JsonObject postgreSQLClientConfig = new JsonObject()
                .put("host", host)
                .put("username", "brownie")
                .put("password", "brownie")
                .put("database", "brownie");
        this.postgreSQLClient = PostgreSQLClient.createShared(vertx, postgreSQLClientConfig);
    }

    @Override
    public void iterate(Handler<AsyncResult<FileMetadataReadStream>> handler) {
        java.util.Objects.requireNonNull(handler);
        Future<FileMetadataReadStream> future = Future.future();
        postgreSQLClient.getConnection(ar -> {
            if (ar.failed()) {
                future.fail(ar.cause());
                handler.handle(future);
                return;
            }
            SQLConnection con = ar.result();
            con.query("SELECT id, name, mime_type, content_length, generated FROM file_metadata",
                    res -> {
                        try (SQLConnection connection = con) {
                            if (res.failed()) {
                                future.fail(res.cause());
                            } else {
                                Iterator<FileMetadata> iterator = res.result().getResults()
                                        .stream()
                                        .map(json -> {
                                            UUID fileId = UUID.fromString(json.getString(0));
                                            MimeType mimeType = MimeType.valueOf(json.getString(2));
                                            Instant generated = Instant.parse(json.getString(4) + "Z");
                                            return new FileMetadata(fileId, json.getString(1), mimeType, json.getLong(3), generated);
                                        })
                                        .iterator();
                                future.complete(new FileMetadataReadStreamImpl(iterator));
                            }
                        }
                        handler.handle(future);
                    });
        });
    }

    @Override
    public void store(FileMetadata metadata,
            Handler<AsyncResult<Void>> handler) {
        Future<Void> future = Future.future();
        postgreSQLClient.getConnection(ar -> {
            if (ar.failed()) {
                future.fail(ar.cause());
                if (handler != null) {
                    handler.handle(future);
                }
                return;
            }
            JsonArray params = new JsonArray()
                    .add(metadata.getFileId().toString())
                    .add(metadata.getName())
                    .add(metadata.getMimeType().toString())
                    .add(metadata.getContentLength())
                    .add(metadata.getGenerated());
            SQLConnection con = ar.result();
            con.updateWithParams("INSERT INTO file_metadata (id, name, mime_type, content_length, generated) VALUES (?, ?, ?, ?, ?)",
                    params,
                    res -> {
                        try (SQLConnection connection = con) {
                            if (res.failed()) {
                                future.fail(res.cause());
                            } else {
                                future.complete();
                            }
                        }
                        if (handler != null) {
                            handler.handle(future);
                        }
            });
        });
    }

    @Override
    public void update(FileMetadata metadata,
            Handler<AsyncResult<Void>> handler) {
        Future<Void> future = Future.future();
        postgreSQLClient.getConnection(ar -> {
            if (ar.failed()) {
                future.fail(ar.cause());
                if (handler != null) {
                    handler.handle(future);
                }
                return;
            }
            JsonArray param = new JsonArray()
                    .add(metadata.getName())
                    .add(metadata.getMimeType().toString())
                    .add(metadata.getContentLength())
                    .add(metadata.getGenerated())
                    .add(metadata.getFileId().toString());
            SQLConnection con = ar.result();
            con.queryWithParams("UPDATE file_metadata SET name = ?, mime_type = ?, content_length = ?, generated  = ? WHERE id = ?",
                    param, res -> {
                        try (SQLConnection connection = con) {
                            if (res.failed()) {
                                future.fail(res.cause());
                            } else if (res.result().getNumRows() == 0){
                                future.fail("No record found");
                            } else {
                                future.complete();
                            }
                        }
                        if (handler != null) {
                            handler.handle(future);
                        }
                    });
        });

    }

    @Override
    public void load(UUID fileId,
            Handler<AsyncResult<Optional<FileMetadata>>> handler) {
        Future<Optional<FileMetadata>> future = Future.future();
        postgreSQLClient.getConnection(ar -> {
            if (ar.failed()) {
                future.fail(ar.cause());
                if (handler != null) {
                    handler.handle(future);
                }
                return;
            }
            JsonArray param = new JsonArray().add(fileId.toString());
            SQLConnection con = ar.result();
            con.queryWithParams("SELECT name, mime_type, content_length, generated FROM file_metadata WHERE id = ?",
                    param, res -> {
                        try (SQLConnection connection = con) {
                            if (res.failed()) {
                                future.fail(res.cause());
                            } else {
                                Optional<FileMetadata> optional = res.result().getRows().stream().findFirst().map(jsonObject -> {
                                    Instant generated = Instant.parse(jsonObject.getString("generated") + "Z");
                                    return new FileMetadata(fileId,
                                            jsonObject.getString("name"),
                                            MimeType.valueOf(jsonObject.getString("mime_type")),
                                            jsonObject.getLong("content_length"),
                                            generated);
                                });
                                future.complete(optional);
                            }
                        }
                        if (handler != null) {
                            handler.handle(future);
                        }
                    });
        });
    }

    @Override
    public void delete(UUID fileId, Handler<AsyncResult<Void>> handler) {
        Future<Void> future = Future.future();
        this.postgreSQLClient.getConnection(ar -> {
            if (ar.failed()) {
                future.fail(ar.cause());
                if (handler != null) {
                    handler.handle(future);
                }
                return;
            }
            JsonArray param = new JsonArray().add(fileId.toString());
            SQLConnection con = ar.result();
            con.setAutoCommit(false, result -> {
                if (result.failed()) {
                    future.fail(result.cause());
                    return;
                }
                con.queryWithParams("SELECT * FROM file_metadata WHERE id = ? FOR UPDATE",
                        param, selected -> {
                    if (selected.failed()) {
                        future.fail(selected.cause());
                        con.rollback(rolled -> {
                            try (SQLConnection connection = con) {
                                if (handler != null) {
                                    handler.handle(future);
                                }
                            }
                        });
                        return;
                    }
                    Integer count = selected.result().getResults().size();
                    if (count == 0) {
                        future.fail("No record found");
                        con.rollback(rolled -> {
                            try (SQLConnection connection = con) {
                                if (handler != null) {
                                    handler.handle(future);
                                }
                            }
                        });
                        return;
                    }
                    con.queryWithParams("DELETE FROM file_metadata WHERE id = ?",
                            param, res -> {
                                if (res.failed()) {
                                    future.fail(res.cause());
                                    con.rollback(rolled -> {
                                        try (SQLConnection connection = con) {
                                            if (handler != null) {
                                                handler.handle(future);
                                            }
                                        }
                                    });
                                } else {
                                    future.complete();
                                    con.commit(commit -> {
                                        try (SQLConnection connection = con) {
                                            if (handler != null) {
                                                handler.handle(future);
                                            }
                                        }
                                    });
                                }
                            });
                });
            });
        });
    }

    @Override
    public void close() {
        this.postgreSQLClient.close();
    }
}
