package jp.skypencil.brownie.registry;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import jp.skypencil.brownie.FileId;
import jp.skypencil.brownie.ThumbnailMetadata;
import rx.Observable;
import rx.Single;

@ParametersAreNonnullByDefault
interface ThumbnailMetadataRegistry {
    /**
     * Store a {@link ThumbnailMetadata} for specified video key.
     *
     * @param videoId
     *      an UUID to specify target video
     * @param metadata
     *      a {@link ThumbnailMetadata} to store into database
     * @return
     */
    @Nonnull
    Single<Void> store(@FileId UUID videoId, ThumbnailMetadata metadata);

    @Nonnull
    Observable<ThumbnailMetadata> search(@FileId UUID videoId);
}
