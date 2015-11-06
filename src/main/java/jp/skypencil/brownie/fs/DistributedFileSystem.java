package jp.skypencil.brownie.fs;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;

import java.util.UUID;

/**
 * Interface to handle files on distributed file system.
 */
public interface DistributedFileSystem {
    void load(UUID key, Handler<AsyncResult<Buffer>> handler);
    void loadAndPipe(UUID key, WriteStream<Buffer> writeStream, Handler<AsyncResult<Void>> handler);
    void store(UUID key, Buffer buffer, Handler<AsyncResult<Void>> handler);
    void pipeToStore(UUID key, ReadStream<Buffer> readStream, Handler<AsyncResult<Void>> handler);
}
