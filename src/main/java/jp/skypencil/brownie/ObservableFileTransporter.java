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
import jp.skypencil.brownie.fs.ObservableSharedFileSystem;
import jp.skypencil.brownie.registry.ObservableFileMetadataRegistry;
import rx.Observable;

@Component
@ParametersAreNonnullByDefault
public class ObservableFileTransporter {
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    @Resource
    private Vertx vertx;

    @Resource
    private ObservableSharedFileSystem fileSystem;

    @Resource
    private ObservableFileMetadataRegistry fileMetadataRegistry;

    /**
     * Download a file from shared file system, and store it to local file system.
     */
    Observable<File> download(UUID key) {
        return store(fileSystem.load(key));
    }

    /**
     * Store data to local file system, and return {@link File} instance
     */
    private Observable<File> store(Observable<Buffer> data) {
        String downloadedFile = TEMP_DIR + "/" + new com.eaio.uuid.UUID();
        return vertx.fileSystem()
            .openObservable(downloadedFile, new OpenOptions().setWrite(true))
            .flatMap(opened -> {
                CompletableFuture<File> future = new CompletableFuture<>();
                data.doAfterTerminate(opened::close)
                    .subscribe(buffer -> {
                        opened.write(buffer);
                    }, error -> {
                        future.completeExceptionally(error);
                    }, () -> {
                        future.complete(new File(downloadedFile));
                    });
                return Observable.from(future);
            });
    }

    Observable<Void> upload(UUID key, String name, File file, MimeType mimeType) {
        return vertx.fileSystem().openObservable(file.getAbsolutePath(),
                new OpenOptions().setRead(true))
        .flatMap(AsyncFile::toObservable)
        .reduce(Buffer.buffer(), (reduced, buffer) -> {
            return reduced.appendBuffer(buffer);
        })
        .flatMap(reducedBuffer -> {
            return fileSystem.store(key, reducedBuffer);
        }).flatMap(v -> {
            FileMetadata metadata = new FileMetadata(key, name, mimeType, file.length(), Instant.now());
            return fileMetadataRegistry.store(metadata);
        });
    }

    Observable<Void> delete(UUID key) {
        return fileMetadataRegistry.delete(key).flatMap(v -> {
            return fileSystem.delete(key);
        });
    }
}
