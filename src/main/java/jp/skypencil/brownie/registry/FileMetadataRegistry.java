package jp.skypencil.brownie.registry;

import java.util.UUID;

import javax.annotation.ParametersAreNonnullByDefault;

import jp.skypencil.brownie.FileId;
import jp.skypencil.brownie.FileMetadata;
import rx.Observable;
import rx.Single;

/**
 * An API to persist/load {@link FileMetadata}.
 */
@ParametersAreNonnullByDefault
public interface FileMetadataRegistry {
    /**
     * Iterate all {@link FileMetadata} in this registry.
     */
    Observable<FileMetadata> iterate();

    /**
     * Store a {@link FileMetadata} to registry. This method fails if given {@link FileMetadata} is already registered.
     * @param metadata
     *      FileMetadata to store
     * @return a {@link Single} which emits stored metadata, or which makes an error notification
     */
    Single<FileMetadata> store(FileMetadata metadata);

    /**
     * Update a {@link FileMetadata} in registry. This method fails if given {@link FileMetadata} is not registered yet.
     * @param metadata
     *      FileMetadata to update
     * @return a {@link Single} which emits updated metadata, or which makes an error notification
     */
    Single<FileMetadata> update(FileMetadata metadata);

    /**
     * Load a {@link FileMetadata} from registry.
     * @param fileId
     *      ID of target {@link FileMetadata} to load
     * @return a {@link Single} which emits loaded metadata, or which makes an error notification
     */
    Single<FileMetadata> load(@FileId UUID fileId);

    /**
     * Delete a {@FileMetadata} from registry. This method fails if no {@link FileMetadata} is registered with given {@link UUID} yet.
     * @param fileId
     *      ID of target {@link FileMetadata} to load
     * @return a {@link Single} which emits fileId, or which makes an error notification
     */
    Single<@FileId UUID> delete(@FileId UUID fileId);
}
