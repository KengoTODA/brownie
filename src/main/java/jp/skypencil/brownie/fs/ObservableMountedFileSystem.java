package jp.skypencil.brownie.fs;

import java.util.Objects;
import java.util.UUID;

import javax.annotation.Resource;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import lombok.RequiredArgsConstructor;
import rx.Observable;

@RequiredArgsConstructor
public class ObservableMountedFileSystem implements ObservableSharedFileSystem {
    private final String baseDir;

    @Resource
    Vertx vertx;

    @Override
    public Observable<Buffer> load(UUID key) {
        String path = baseDir + "/" + Objects.requireNonNull(key);
        return vertx.fileSystem().readFileObservable(path);
    }

    @Override
    public Observable<Void> store(UUID key, Buffer buffer) {
        String path = baseDir + "/" + Objects.requireNonNull(key);
        return vertx.fileSystem().writeFileObservable(path, buffer);
    }

    @Override
    public Observable<Void> delete(UUID key) {
        String path = baseDir + "/" + Objects.requireNonNull(key);
        return vertx.fileSystem().deleteObservable(path);
    }
}
