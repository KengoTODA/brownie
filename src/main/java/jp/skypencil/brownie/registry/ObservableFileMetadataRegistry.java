package jp.skypencil.brownie.registry;

import java.util.UUID;

import javax.annotation.ParametersAreNonnullByDefault;

import jp.skypencil.brownie.FileId;
import jp.skypencil.brownie.FileMetadata;
import rx.Observable;

/**
 * An API to persist/load {@link FileMetadata}.
 */
@ParametersAreNonnullByDefault
public interface ObservableFileMetadataRegistry extends AutoCloseable {
    /**
     * Iterate all {@link FileMetadata} in this registry.
     */
    Observable<FileMetadata> iterate();

    /**
     * Store a {@link FileMetadata} to registry. This method fails if given {@link FileMetadata} is already registered.
     * @param metadata
     *      FileMetadata to store
     */
    Observable<Void> store(FileMetadata metadata);

    /**
     * Update a {@link FileMetadata} in registry. This method fails is given {@link FileMetadata} is not registered yet.
     * @param metadata
     *      FileMetadata to update
     */
    Observable<Void> update(FileMetadata metadata);

    /**
     * Load a {@link FileMetadata} from registry.
     * @param fileId
     *      ID of target {@link FileMetadata} to load
     */
    Observable<FileMetadata> load(@FileId UUID fileId);

    /**
     * Delete a {@FileMetadata} from registry.
     * @param fileId
     *      ID of target {@link FileMetadata} to load
     */
    Observable<Void> delete(@FileId UUID fileId);

    @Override
    void close();
}
