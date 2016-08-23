package jp.skypencil.brownie.fs;

import java.util.Objects;
import java.util.UUID;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import lombok.RequiredArgsConstructor;
import rx.Observable;

@RequiredArgsConstructor
public class MountedFileSystem implements SharedFileSystem {
    private final String baseDir;

    private final Vertx rxJavaVertx;

    @Override
    public Observable<Buffer> load(UUID key) {
        String path = baseDir + "/" + Objects.requireNonNull(key);
        return rxJavaVertx.fileSystem().readFileObservable(path);
    }

    @Override
    public Observable<Void> store(UUID key, Buffer buffer) {
        String path = baseDir + "/" + Objects.requireNonNull(key);
        return rxJavaVertx.fileSystem().writeFileObservable(path, buffer);
    }

    @Override
    public Observable<Void> delete(UUID key) {
        String path = baseDir + "/" + Objects.requireNonNull(key);
        return rxJavaVertx.fileSystem().deleteObservable(path);
    }
}
