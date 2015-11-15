package jp.skypencil.brownie.fs;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.Pump;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;

import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.Resource;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MountedFileSystem implements SharedFileSystem {
    private final String baseDir;

    @Resource
    Vertx vertx;

    @Override
    public void load(UUID key, Handler<AsyncResult<Buffer>> handler) {
        String path = baseDir + "/" + Objects.requireNonNull(key);
        vertx.fileSystem().readFile(path, handler);
    }

    @Override
    public void store(UUID key, Buffer buffer, Handler<AsyncResult<Void>> handler) {
        String path = baseDir + "/" + Objects.requireNonNull(key);
        vertx.fileSystem().writeFile(path, buffer, handler);
    }

    @Override
    public void loadAndPipe(UUID key, WriteStream<Buffer> writeStream, Handler<AsyncResult<Void>> givenHandler) {
        Objects.requireNonNull(writeStream);
        String path = baseDir + "/" + Objects.requireNonNull(key);
        Future<Void> future = Future.future();
        final Handler<AsyncResult<Void>> handler =
                firstNonNull(givenHandler, result -> {});
        vertx.fileSystem().open(path, new OpenOptions().setRead(true), result -> {
            if (result.failed()) {
                future.fail(result.cause());
                handler.handle(future);
            } else {
                AsyncFile readStream = result.result();
                writeStream.exceptionHandler(throwable -> {
                    future.fail(throwable);
                    handler.handle(future);
                });
                readStream.exceptionHandler(throwable -> {
                    future.fail(throwable);
                    handler.handle(future);
                }).endHandler(end -> {
                    future.complete();
                    handler.handle(future);
                });
                Pump.pump(readStream, writeStream).start();
            }
        });
    }

    @Override
    public void pipeToStore(UUID key, ReadStream<Buffer> readStream,
            Handler<AsyncResult<Void>> givenHandler) {
        Objects.requireNonNull(readStream);
        String path = baseDir + "/" + Objects.requireNonNull(key);
        Future<Void> future = Future.future();
        final Handler<AsyncResult<Void>> handler =
                firstNonNull(givenHandler, result -> {});
        vertx.fileSystem().open(path, new OpenOptions().setWrite(true), opened -> {
            if (opened.failed()) {
                future.fail(opened.cause());
                handler.handle(future);
            } else {
                AsyncFile writeStream = opened.result();
                writeStream.exceptionHandler(throwable -> {
                    future.fail(throwable);
                    handler.handle(future);
                });
                readStream.exceptionHandler(throwable -> {
                    future.fail(throwable);
                    handler.handle(future);
                }).endHandler(end -> {
                    future.complete();
                    handler.handle(future);
                });
                Pump.pump(readStream, writeStream).start();
            }
        });
    }

    private <T> Handler<T> firstNonNull(@Nullable Handler<T> first,
            @Nonnull Handler<T> second) {
        if (first == null) {
            return Objects.requireNonNull(second);
        } else {
            return first;
        }
    }
}
