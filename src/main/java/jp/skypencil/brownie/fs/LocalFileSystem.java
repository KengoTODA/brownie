package jp.skypencil.brownie.fs;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;

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
