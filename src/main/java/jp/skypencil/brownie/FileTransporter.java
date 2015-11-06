package jp.skypencil.brownie;

import java.io.File;
import java.util.UUID;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.file.OpenOptions;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.Resource;

import jp.skypencil.brownie.fs.DistributedFileSystem;

import org.springframework.stereotype.Component;

@Component
@ParametersAreNonnullByDefault
public class FileTransporter {
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    @Resource
    private Vertx vertx;

    @Resource
    private DistributedFileSystem fileSystem;

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

    void upload(UUID key, File file, Handler<AsyncResult<Void>> handler) {
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
                        } else {
                            future.complete();
                        }
                        handler.handle(future);
                    });
                });
    }
}
