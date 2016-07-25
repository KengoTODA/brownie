package jp.skypencil.brownie.registry;

import java.util.Iterator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;
import jp.skypencil.brownie.Task;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor class TaskReadStream implements ReadStream<Task> {
    @Nonnull
    private final Iterator<Task> source;
    @Nullable
    private Handler<Task> handler;
    private boolean paused;
    @Nullable
    private Handler<Void> endHandler;

    @Override
    public ReadStream<Task> exceptionHandler(Handler<Throwable> handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ReadStream<Task> handler(Handler<Task> handler) {
        this.handler = handler;
        if (handler != null) {
            doRead();
        }
        return this;
    }

    @Override
    public ReadStream<Task> pause() {
        this.paused = true;
        return this;
    }

    @Override
    public ReadStream<Task> resume() {
        this.paused = false;
        if (handler != null) {
            doRead();
        }
        return this;
    }

    @Override
    public ReadStream<Task> endHandler(Handler<Void> endHandler) {
        this.endHandler = endHandler;
        return this;
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