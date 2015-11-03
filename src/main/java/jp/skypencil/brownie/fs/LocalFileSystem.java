package jp.skypencil.brownie.fs;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.Pump;
import io.vertx.core.streams.WriteStream;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LocalFileSystem implements DistributedFileSystem {
    private final String baseDir = createDirectory();

    @Resource
    Vertx vertx;

    @Override
    public void load(UUID key, Handler<AsyncResult<Buffer>> handler) {
        String path = baseDir + "/" + key;
        vertx.fileSystem().readFile(path, handler);
    }

    @Override
    public void store(UUID key, Buffer buffer, Handler<AsyncResult<Void>> handler) {
        String path = baseDir + "/" + key;
        vertx.fileSystem().writeFile(path, buffer, handler);
    }

    @Override
    public void loadAndPipe(UUID key, WriteStream<Buffer> writeStream, Handler<AsyncResult<Void>> handler) {
        String path = baseDir + "/" + key;
        Future<Void> future = Future.future();
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

    private String createDirectory() {
        try {
            Path directory = Files.createTempDirectory("brownie");
            log.debug("Directory to store file is created at {}", directory);
            return directory.toFile().getAbsolutePath();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
