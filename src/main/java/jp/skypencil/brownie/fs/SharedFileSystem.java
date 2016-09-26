package jp.skypencil.brownie.fs;

import java.util.UUID;

import javax.annotation.ParametersAreNonnullByDefault;

import io.vertx.rxjava.core.buffer.Buffer;
import jp.skypencil.brownie.FileMetadata;
import jp.skypencil.brownie.FileTransporter;
import rx.Observable;
import rx.Single;

/**
 * Interface to handle files on shared file system.
 * Service code should use {@link FileTransporter} instead of this interface, to update {@link FileMetadata} at the same time.
 */
@ParametersAreNonnullByDefault
public interface SharedFileSystem {
    /**
     * Load a file from shared file system. This operation fails if target file does not exist.
     *
     * @param id
     *      ID of file to load
     */
    Observable<Buffer> load(UUID id);

    /**
     * Store a file to shared file system. This operation fails if target file already exists.
     *
     * @param id
     *      IDof file on shared file system to store
     * @param buffer
     *      Target {@link Buffer} to store
     */
    Single<Void> store(UUID id, Buffer buffer);

    /**
     * Delete a file stored on shared file system.
     *
     * @param id
     *      ID of file on shared file system to store
     */
    Single<Void> delete(UUID id);
}
