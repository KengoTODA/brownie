package jp.skypencil.brownie.fs;

import java.util.UUID;

import javax.annotation.ParametersAreNonnullByDefault;

import io.vertx.rxjava.core.buffer.Buffer;
import jp.skypencil.brownie.FileMetadata;
import jp.skypencil.brownie.ObservableFileTransporter;
import rx.Observable;

/**
 * Interface to handle files on shared file system.
 * Service code should use {@link ObservableFileTransporter} instead of this interface, to update {@link FileMetadata} at the same time.
 */
@ParametersAreNonnullByDefault
public interface ObservableSharedFileSystem {
    /**
     * Load a file from shared file system. This operation fails if target file does not exist.
     *
     * @param key
     *      Key of file to load
     */
    Observable<Buffer> load(UUID key);

    /**
     * Store a file to shared file system. This operation fails if target file already exists.
     *
     * @param key
     *      Key of file on shared file system to store
     * @param buffer
     *      Target {@link Buffer} to store
     */
    Observable<Void> store(UUID key, Buffer buffer);

    /**
     * Delete a file stored on shared file system.
     *
     * @param key
     *      Key of file on shared file system to store
     */
    Observable<Void> delete(UUID key);
}
