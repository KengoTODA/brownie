package jp.skypencil.brownie;

import java.io.File;
import java.util.UUID;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.file.OpenOptions;

import javax.annotation.Resource;

import jp.skypencil.brownie.fs.DistributedFileSystem;

import org.springframework.stereotype.Component;

@Component
public class FileDownloader {
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    @Resource
    private Vertx vertx;

    @Resource
    private DistributedFileSystem fileSystem;

    void download(UUID key, Handler<AsyncResult<File>> handler) {
        String downloadedFile = TEMP_DIR + "/" + new com.eaio.uuid.UUID();
        Future<File> future = Future.future();
        vertx.fileSystem().open(downloadedFile,
                new OpenOptions().setWrite(true), result -> {
                    if (result.failed()) {
                        future.fail(result.cause());
                        handler.handle(future);
                        return;
                    }

                    fileSystem.loadAndPipe(key, result.result(), finished -> {
                        if (finished.failed()) {
                            future.fail(finished.cause());
                        } else {
                            future.complete(new File(downloadedFile));
                        }
                        handler.handle(future);
                    });
                });
    }
}
