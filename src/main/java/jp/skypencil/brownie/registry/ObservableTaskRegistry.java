package jp.skypencil.brownie.registry;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import jp.skypencil.brownie.Task;
import rx.Observable;

/**
 * An API to persist/load {@link Task}s.
 */
@ParametersAreNonnullByDefault
public interface ObservableTaskRegistry extends AutoCloseable {
    /**
     * Iterate all {@link Task}s in this registry. Result is sorted by updated time.
     */
    @Nonnull
    Observable<Task> iterate();

    /**
     * Store a {@link Task} to registry. This method fails if given {@link Task} is already registered.
     * @param task
     *      Task to register
     */
    @Nonnull
    Observable<Void> store(Task task);

    /**
     * Load a {@link Task} from registry.
     * @param taskId
     *      ID of target {@link Task} to load
     */
    @Nonnull
    Observable<Optional<Task>> load(UUID taskId);

    @Override
    void close();
}
