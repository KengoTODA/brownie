package jp.skypencil.brownie.registry;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import jp.skypencil.brownie.Task;

/**
 * An API to persist/load {@link Task}s.
 */
@ParametersAreNonnullByDefault
public interface TaskRegistry {
    /**
     * Iterate all {@link Task}s in this registry. Result is sorted by updated time.
     */
    void iterate(@Nonnull Handler<AsyncResult<ReadStream<Task>>> handler);

    /**
     * Store a {@link Task} to registry. This method fails if given {@link Task} is already registered.
     * @param task
     *      Task to register
     * @param handler
     *      Callback to handle result
     */
    void store(Task task, @Nullable Handler<AsyncResult<Void>> handler);

    /**
     * Load a {@link Task} from registry. 
     * @param taskId
     *      ID of target {@link Task} to load
     * @param handler
     *      Callback to handle result
     */
    void load(UUID taskId, @Nullable Handler<AsyncResult<Optional<Task>>> handler);
}
