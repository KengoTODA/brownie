package jp.skypencil.brownie;

import java.io.File;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.Resource;

import org.springframework.util.MimeType;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.WriteStream;
import jp.skypencil.brownie.fs.SharedFileSystem;
import jp.skypencil.brownie.registry.FileMetadataRegistry;

@ParametersAreNonnullByDefault
public class FileTransporter {
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    @Resource
    private Vertx vertx;

    @Resource
    private SharedFileSystem fileSystem;

    @Resource
    private FileMetadataRegistry fileMetadataRegistry;

    void download(UUID key, Handler<AsyncResult<File>> handler) {
        String downloadedFile = TEMP_DIR + "/" + new com.eaio.uuid.UUID();
        Future<File> future = Future.future();
        vertx.fileSystem().open(downloadedFile,
                new OpenOptions().setWrite(true),
                opened -> {
                    if (opened.failed()) {
                        future.fail(opened.cause());
                        handler.handle(future);
                        return;
                    }

                    fileSystem.loadAndPipe(key, opened.result(), finished -> {
                        if (finished.failed()) {
                            future.fail(finished.cause());
                        } else {
                            future.complete(new File(downloadedFile));
                        }
                        handler.handle(future);
                    });
                });
    }

    void upload(UUID key, String name, File file, MimeType mimeType, Handler<AsyncResult<Void>> handler) {
        Future<Void> future = Future.future();
        vertx.fileSystem().open(file.getAbsolutePath(),
                new OpenOptions().setRead(true),
                opened -> {
                    if (opened.failed()) {
                        future.fail(opened.cause());
                        handler.handle(future);
                        return;
                    }
                    fileSystem.pipeToStore(key, opened.result(), stored -> {
                        if (stored.failed()) {
                            future.fail(stored.cause());
                            handler.handle(future);
                            return;
                        }
                        FileMetadata metadata = new FileMetadata(key, name, mimeType, file.length(), Instant.now());
                        fileMetadataRegistry.store(metadata, metadataStored -> {
                            if (metadataStored.failed()) {
                                future.fail(metadataStored.cause());
                            } else {
                                future.complete();
                            }
                            handler.handle(future);
                        });
                    });
                });
    }

    void downloadToPipe(UUID key, WriteStream<Buffer> buffer, @Nullable Handler<AsyncResult<Void>> handler) {
        fileSystem.loadAndPipe(key, buffer, handler);
    }

    void delete(UUID key, Handler<AsyncResult<Void>> handler) {
        AtomicInteger finished = new AtomicInteger(2);
        Future<Void> future = Future.future();
        fileMetadataRegistry.delete(key, metadataDeleted -> {
            if (metadataDeleted.failed()) {
                future.fail(metadataDeleted.cause());
                handler.handle(future);
            }
            if (finished.decrementAndGet() == 0 && !future.isComplete()) {
                future.complete();
                handler.handle(future);
            }
        });
        fileSystem.delete(key, fileDeleted -> {
            if (fileDeleted.failed()) {
                future.fail(fileDeleted.cause());
                handler.handle(future);
            }
            if (finished.decrementAndGet() == 0 && !future.isComplete()) {
                future.complete();
                handler.handle(future);
            }
        });
    }
}
