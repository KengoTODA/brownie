package jp.skypencil.brownie.registry;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;
import jp.skypencil.brownie.Task;

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
        if (data.putIfAbsent(task.getKey(), task) != null) {
            future.fail("Given task is already registered");
        } else {
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

}
