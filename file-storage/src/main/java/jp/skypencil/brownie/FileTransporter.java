package jp.skypencil.brownie;

import java.io.File;
import java.time.Instant;
import java.util.UUID;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;

import io.vertx.core.file.OpenOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.file.AsyncFile;
import jp.skypencil.brownie.fs.SharedFileSystem;
import jp.skypencil.brownie.registry.FileMetadataRegistry;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import rx.Observable;
import rx.Single;
import scala.Tuple2;

@ParametersAreNonnullByDefault
@RequiredArgsConstructor(
        onConstructor = @__(@Inject),
        access = AccessLevel.PACKAGE) // only for unit test
public class FileTransporter {
    private final Vertx rxJavaVertx;

    private final SharedFileSystem observableFileSystem;

    private final FileMetadataRegistry observableFileMetadataRegistry;

    /**
     * Download a file from shared file system, and store it to local file system.
     * Caller should close returned {@link AsyncFile}.
     */
    Single<Tuple2<FileMetadata, File>> download(UUID id) {
        return observableFileMetadataRegistry.load(id)
            .zipWith(observableFileSystem.load(id), Tuple2::apply);
    }

    Single<FileMetadata> upload(UUID id, String name, File file, MimeType mimeType) {
        return rxJavaVertx.fileSystem().openObservable(file.getAbsolutePath(),
                new OpenOptions().setRead(true))
        .flatMap(AsyncFile::toObservable)
        .reduce(Buffer.buffer(), (reduced, buffer) -> {
            // FIXME consumes too much memory
            return reduced.appendBuffer(buffer);
        })
        .toSingle()
        .flatMap(reducedBuffer -> {
            return observableFileSystem.store(id, reducedBuffer);
        }).flatMap(v -> {
            FileMetadata metadata = new FileMetadata(id, name, mimeType, file.length(), Instant.now());
            return observableFileMetadataRegistry.store(metadata);
        });
    }

    Single<@FileId UUID> delete(@FileId UUID id) {
        return observableFileMetadataRegistry.delete(id).flatMap(v -> {
            return observableFileSystem.delete(id);
        });
    }

    Observable<FileMetadata> list() {
        return observableFileMetadataRegistry.iterate();
    }
}
