package jp.skypencil.brownie.registry;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import jp.skypencil.brownie.event.VideoUploadedEvent;
import rx.Observable;
import rx.Single;

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
     * @param event
     *      Event to register
     */
    @Nonnull
    Single<Void> store(VideoUploadedEvent event);

    /**
     * Load a {@link VideoUploadedEvent} from registry.
     * @param id
     *      ID of target {@link VideoUploadedEvent} to load
     */
    @Nonnull
    Single<VideoUploadedEvent> load(UUID id);
}
