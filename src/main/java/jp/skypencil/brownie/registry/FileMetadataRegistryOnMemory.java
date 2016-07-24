package jp.skypencil.brownie.registry;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

import jp.skypencil.brownie.FileMetadata;

/**
 * An implementation of {@link FileMetadataRegistry}, which stores data on memory.
 */
@Component
public class FileMetadataRegistryOnMemory implements FileMetadataRegistry {
    private final ConcurrentMap<UUID, FileMetadata> data = new ConcurrentHashMap<>();

    @Override
    public void iterate(Handler<AsyncResult<FileMetadataReadStream>> handler) {
        Objects.requireNonNull(handler);
        FileMetadataReadStream readStream = new FileMetadataReadStreamImpl(data.values().iterator());
        handler.handle(Future.succeededFuture(readStream));
    }

    @Override
    public void store(FileMetadata metadata, Handler<AsyncResult<Void>> handler) {
        Objects.requireNonNull(metadata);
        Future<Void> future = Future.future();
        if (data.putIfAbsent(metadata.getFileId(), metadata) != null) {
            future.fail("Target metadata is already registered");
        } else {
            future.complete();
        }

        if (handler != null) {
            handler.handle(future);
        }
    }

    @Override
    public void update(FileMetadata metadata, Handler<AsyncResult<Void>> handler) {
        Objects.requireNonNull(metadata);
        Future<Void> future = Future.future();
        if (!data.containsKey(metadata.getFileId())) {
            future.fail("No metadata found to update");
        } else {
            data.put(metadata.getFileId(), metadata);
            future.complete();
        }

        if (handler != null) {
            handler.handle(future);
        }
    }

    @Override
    public void load(UUID fileId, Handler<AsyncResult<Optional<FileMetadata>>> handler) {
        Objects.requireNonNull(fileId);
        Objects.requireNonNull(handler);
        FileMetadata loaded = data.get(fileId);
        Future<Optional<FileMetadata>> future = Future.succeededFuture(Optional.ofNullable(loaded));
        handler.handle(future);
    }

    @Override
    public void delete(UUID fileId, Handler<AsyncResult<Void>> handler) {
        final Future<Void> future;
        if (data.remove(fileId) == null) {
            future = Future.failedFuture("No metadata found to delete");
        } else {
            future = Future.succeededFuture();
        }
        handler.handle(future);
    }
}
