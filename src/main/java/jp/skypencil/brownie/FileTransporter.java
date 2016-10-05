package jp.skypencil.brownie;

import java.io.File;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    private final Vertx rxJavaVertx;

    private final SharedFileSystem observableFileSystem;

    private final FileMetadataRegistry observableFileMetadataRegistry;

    /**
     * Download a file from shared file system, and store it to local file system.
     */
    Single<Tuple2<FileMetadata, File>> download(UUID id) {
        return observableFileMetadataRegistry.load(id)
            .zipWith(store(observableFileSystem.load(id)), Tuple2::apply);
    }

    /**
     * Store data to local file system, and return {@link File} instance
     */
    private Single<File> store(Observable<Buffer> data) {
        String downloadedFile = TEMP_DIR + "/" + new com.eaio.uuid.UUID();
        return rxJavaVertx.fileSystem()
            .openObservable(downloadedFile, new OpenOptions().setWrite(true))
            .toSingle()
            .flatMap(opened -> {
                CompletableFuture<File> future = new CompletableFuture<>();
                data.subscribe(buffer -> {
                        opened.write(buffer);
                    }, error -> {
                        opened.close();
                        future.completeExceptionally(error);
                    }, () -> {
                        opened.close();
                        future.complete(new File(downloadedFile));
                    });
                return Single.from(future);
            });
    }

    Single<FileMetadata> upload(UUID id, String name, File file, MimeType mimeType) {
        return rxJavaVertx.fileSystem().openObservable(file.getAbsolutePath(),
                new OpenOptions().setRead(true))
        .flatMap(AsyncFile::toObservable)
        .reduce(Buffer.buffer(), (reduced, buffer) -> {
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
}
