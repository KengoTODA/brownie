package jp.skypencil.brownie.registry;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import jp.skypencil.brownie.FileId;
import jp.skypencil.brownie.FileMetadata;

/**
 * An API to persist/load {@link FileMetadata}.
 */
@ParametersAreNonnullByDefault
public interface FileMetadataRegistry {
    /**
     * Iterate all {@link FileMetadata} in this registry. Result is sorted by generated time.
     * @param handler
     *      Callback to handle result
     */
    void iterate(Handler<AsyncResult<FileMetadataReadStream>> handler);

    /**
     * Store a {@link FileMetadata} to registry. This method fails if given {@link FileMetadata} is already registered.
     * @param metadata
     *      FileMetadata to store
     * @param handler
     *      Callback to handle result
     */
    void store(FileMetadata metadata, @Nullable Handler<AsyncResult<Void>> handler);

    /**
     * Update a {@link FileMetadata} in registry. This method fails is given {@link FileMetadata} is not registered yet.
     * @param metadata
     *      FileMetadata to update
     * @param handler
     *      Callback to handle result
     */
    void update(FileMetadata metadata, @Nullable Handler<AsyncResult<Void>> handler);

    /**
     * Load a {@link FileMetadata} from registry. 
     * @param fileId
     *      ID of target {@link FileMetadata} to load
     * @param handler
     *      Callback to handle result
     */
    void load(@FileId UUID fileId, Handler<AsyncResult<Optional<FileMetadata>>> handler);

    /**
     * Delete a {@FileMetadata} from registry.
     * @param fileId
     *      ID of target {@link FileMetadata} to load
     * @param handler
     *      Callback to handle result
     */
    void delete(@FileId UUID fileId, @Nullable Handler<AsyncResult<Void>> handler);
}
