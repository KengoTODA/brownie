package jp.skypencil.brownie.fs;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;

import java.util.UUID;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import jp.skypencil.brownie.FileMetadata;
import jp.skypencil.brownie.FileTransporter;

/**
 * Interface to handle files on distributed file system.
 * Service code should use {@link FileTransporter} instead of this interface, to update {@link FileMetadata} at the same time.
 */
@ParametersAreNonnullByDefault
public interface DistributedFileSystem {
    /**
     * Load a file from distributed file system. This operation fails if target file does not exist.
     *
     * @param key
     *      Key of file to load
     * @param handler
     *      Handler to handle data as {@link Buffer}
     */
    void load(UUID key, @Nullable Handler<AsyncResult<Buffer>> handler);

    /**
     * Load a file from distributed file system, and pipe it to given {@link WriteStream}.
     * This operation fails if target file does not exist.
     *
     * @param key
     *      Key of file to load
     * @param writeStream
     *      Target {@link WriteStream} to pipe loaded data
     * @param handler
     *      Callback function to handle result
     */
    void loadAndPipe(UUID key, WriteStream<Buffer> writeStream, @Nullable Handler<AsyncResult<Void>> handler);

    /**
     * Store a file to distributed file system. This operation fails if target file already exists.
     *
     * @param key
     *      Key of file on distributed file system to store
     * @param buffer
     *      Target {@link Buffer} to store
     * @param handler
     *      Callback function to handle result
     */
    void store(UUID key, Buffer buffer, @Nullable Handler<AsyncResult<Void>> handler);

    /**
     * Store a file to distributed file system, from given {@link ReadStream}.
     * This operation fails if target file already exists.
     *
     * @param key
     *      Key of file on distributed file system to store
     * @param readStream
     *      Source {@link ReadStream} to pipe
     * @param handler
     *      Callback function to handle result
     */
    void pipeToStore(UUID key, ReadStream<Buffer> readStream, @Nullable Handler<AsyncResult<Void>> handler);
}
