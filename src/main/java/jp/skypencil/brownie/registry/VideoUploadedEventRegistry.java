package jp.skypencil.brownie.registry;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import jp.skypencil.brownie.event.VideoUploadedEvent;
import rx.Observable;

/**
 * An API to persist/load {@link VideoUploadedEvent}s.
 */
@ParametersAreNonnullByDefault
public interface VideoUploadedEventRegistry {
    /**
     * Iterate all {@link VideoUploadedEvent}s in this registry. Result is sorted by updated time.
     */
    @Nonnull
    Observable<VideoUploadedEvent> iterate();

    /**
     * Store a {@link VideoUploadedEvent} to registry. This method fails if given {@link VideoUploadedEvent} is already registered.
     * @param task
     *      Task to register
     */
    @Nonnull
    Observable<Object> store(VideoUploadedEvent task);

    /**
     * Load a {@link VideoUploadedEvent} from registry.
     * @param taskId
     *      ID of target {@link VideoUploadedEvent} to load
     */
    @Nonnull
    Observable<Optional<VideoUploadedEvent>> load(UUID taskId);
}
