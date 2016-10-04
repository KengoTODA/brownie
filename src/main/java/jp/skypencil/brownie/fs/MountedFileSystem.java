package jp.skypencil.brownie.fs;

import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.file.FileSystem;
import rx.Observable;
import rx.Single;

public class MountedFileSystem implements SharedFileSystem {
    private final String baseDir;

    private final Vertx rxJavaVertx;

    @Inject
    MountedFileSystem(@Named("mountedDir") String baseDir, Vertx vertx) {
        this.baseDir = baseDir;
        this.rxJavaVertx = vertx;
    }

    @Override
    public Observable<Buffer> load(UUID id) {
        String path = baseDir + "/" + Objects.requireNonNull(id);
        return rxJavaVertx.fileSystem().readFileObservable(path);
    }

    @Override
    public Single<UUID> store(UUID id, Buffer buffer) {
        String path = baseDir + "/" + Objects.requireNonNull(id);
        FileSystem fileSystem = rxJavaVertx.fileSystem();

        return fileSystem.existsObservable(path).toSingle().flatMap(found -> {
            if (found.booleanValue()) {
                throw new IllegalArgumentException("Specified ID is already used: " + id);
            }
            return fileSystem.writeFileObservable(path, buffer).toSingle()
                    .map(v -> id);
        });
    }

    @Override
    public Single<UUID> delete(UUID id) {
        String path = baseDir + "/" + Objects.requireNonNull(id);
        return rxJavaVertx.fileSystem().deleteObservable(path).toSingle()
                .map(v -> id);
    }
}
