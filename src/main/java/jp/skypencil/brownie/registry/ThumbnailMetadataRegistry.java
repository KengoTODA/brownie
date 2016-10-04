package jp.skypencil.brownie.registry;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import jp.skypencil.brownie.ThumbnailMetadata;
import rx.Observable;
import rx.Single;

@ParametersAreNonnullByDefault
public interface ThumbnailMetadataRegistry {
    /**
     * Store a {@link ThumbnailMetadata}.
     *
     * @param metadata
     *      a {@link ThumbnailMetadata} to store into database
     * @return a {@link Single} which emits stored metadata, or which makes an error notification
     */
    @Nonnull
    Single<ThumbnailMetadata> store(ThumbnailMetadata metadata);

    @Nonnull
    Observable<ThumbnailMetadata> search(UUID videoId);
}
