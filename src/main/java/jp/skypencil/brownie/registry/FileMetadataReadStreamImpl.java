package jp.skypencil.brownie.registry;

import java.util.Iterator;
import java.util.Objects;

import javax.annotation.Nullable;

import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;
import jp.skypencil.brownie.FileMetadata;

final class FileMetadataReadStreamImpl implements FileMetadataReadStream {
    private final Iterator<FileMetadata> source;
    private boolean paused;
    @Nullable
    private Handler<FileMetadata> handler;
    @Nullable
    private Handler<Void> endHandler;
    @Nullable
    private Handler<Throwable> exceptionHandler;

    FileMetadataReadStreamImpl(Iterator<FileMetadata> source) {
        this.source = Objects.requireNonNull(source);
    }

    @Override
    public ReadStream<FileMetadata> exceptionHandler(
            Handler<Throwable> handler) {
        this.exceptionHandler = handler;
        return this;
    }

    @Override
    public ReadStream<FileMetadata> handler(Handler<FileMetadata> handler) {
        this.handler = handler;
        doRead();
        return this;
    }

    @Override
    public ReadStream<FileMetadata> pause() {
        paused = true;
        return this;
    }

    @Override
    public ReadStream<FileMetadata> resume() {
        paused = false;
        doRead();
        return this;
    }

    @Override
    public ReadStream<FileMetadata> endHandler(Handler<Void> endHandler) {
        this.endHandler = endHandler;
        return this;
    }

    @Override
    public void close() {
        // do nothing
    }

    private void doRead() {
        while (!paused && handler != null) {
            if (!source.hasNext()) {
                if (endHandler != null) {
                    endHandler.handle(null);
                }
                return;
            }
            handler.handle(source.next());
        }
    }
}