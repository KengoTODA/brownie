package jp.skypencil.brownie.fs;

import java.util.Objects;
import java.util.UUID;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import lombok.RequiredArgsConstructor;
import rx.Observable;
import rx.Single;

@RequiredArgsConstructor
public class MountedFileSystem implements SharedFileSystem {
    private final String baseDir;

    private final Vertx rxJavaVertx;

    @Override
    public Observable<Buffer> load(UUID id) {
        String path = baseDir + "/" + Objects.requireNonNull(id);
        return rxJavaVertx.fileSystem().readFileObservable(path);
    }

    @Override
    public Single<Void> store(UUID id, Buffer buffer) {
        String path = baseDir + "/" + Objects.requireNonNull(id);
        return rxJavaVertx.fileSystem().writeFileObservable(path, buffer).toSingle();
    }

    @Override
    public Single<Void> delete(UUID id) {
        String path = baseDir + "/" + Objects.requireNonNull(id);
        return rxJavaVertx.fileSystem().deleteObservable(path).toSingle();
    }
}
