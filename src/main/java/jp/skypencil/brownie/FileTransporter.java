package jp.skypencil.brownie;

import java.io.File;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.Resource;

import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

import io.vertx.core.file.OpenOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.file.AsyncFile;
import jp.skypencil.brownie.fs.SharedFileSystem;
import jp.skypencil.brownie.registry.FileMetadataRegistry;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import rx.Observable;

@Component
@ParametersAreNonnullByDefault
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE) // only for unit test
public class FileTransporter {
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    @Resource
    private Vertx rxJavaVertx;

    @Resource
    private SharedFileSystem observableFileSystem;

    @Resource
    private FileMetadataRegistry observableFileMetadataRegistry;

    /**
     * Download a file from shared file system, and store it to local file system.
     */
    Observable<File> download(UUID key) {
        return store(observableFileSystem.load(key));
    }

    /**
     * Store data to local file system, and return {@link File} instance
     */
    private Observable<File> store(Observable<Buffer> data) {
        String downloadedFile = TEMP_DIR + "/" + new com.eaio.uuid.UUID();
        return rxJavaVertx.fileSystem()
            .openObservable(downloadedFile, new OpenOptions().setWrite(true))
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
                return Observable.from(future);
            });
    }

    Observable<Void> upload(UUID key, String name, File file, MimeType mimeType) {
        return rxJavaVertx.fileSystem().openObservable(file.getAbsolutePath(),
                new OpenOptions().setRead(true))
        .flatMap(AsyncFile::toObservable)
        .reduce(Buffer.buffer(), (reduced, buffer) -> {
            return reduced.appendBuffer(buffer);
        })
        .flatMap(reducedBuffer -> {
            return observableFileSystem.store(key, reducedBuffer);
        }).flatMap(v -> {
            FileMetadata metadata = new FileMetadata(key, name, mimeType, file.length(), Instant.now());
            return observableFileMetadataRegistry.store(metadata);
        });
    }

    Observable<Void> delete(UUID key) {
        return observableFileMetadataRegistry.delete(key).flatMap(v -> {
            return observableFileSystem.delete(key);
        });
    }
}
