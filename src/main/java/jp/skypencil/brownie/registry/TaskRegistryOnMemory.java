package jp.skypencil.brownie.registry;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import jp.skypencil.brownie.Task;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
class TaskRegistryOnMemory implements TaskRegistry {
    private final ConcurrentMap<UUID, Task> data = new ConcurrentHashMap<>();

    @Override
    public void iterate(Handler<AsyncResult<ReadStream<Task>>> handler) {
        Objects.requireNonNull(handler);
        Future<ReadStream<Task>> future =
                Future.succeededFuture(new TaskReadStream(data.values().iterator()));
        handler.handle(future);
    }

    @Override
    public void store(Task task, Handler<AsyncResult<Void>> givenHandler) {
        Objects.requireNonNull(task);
        Future<Void> future = Future.future();
        final Handler<AsyncResult<Void>> handler =
                firstNonNull(givenHandler, result -> {});
        if (data.containsKey(task.getKey())) {
            future.fail("Given task is already registered");
        } else {
            data.put(task.getKey(), task);
            future.complete();
        }
        handler.handle(future);
    }

    @Override
    public void load(UUID taskId, Handler<AsyncResult<Optional<Task>>> givenHandler) {
        Objects.requireNonNull(taskId);
        Future<Optional<Task>> future = Future.future();
        final Handler<AsyncResult<Optional<Task>>> handler =
                firstNonNull(givenHandler, result -> {});
        Optional<Task> result = Optional.ofNullable(data.get(taskId));
        future.complete(result);
        handler.handle(future);
    }

    @Nonnull
    private <T> Handler<T> firstNonNull(@Nullable Handler<T> first,
            @Nonnull Handler<T> second) {
        if (first == null) {
            return Objects.requireNonNull(second);
        } else {
            return first;
        }
    }

    @RequiredArgsConstructor
    private static class TaskReadStream implements ReadStream<Task> {
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

}
