package jp.skypencil.brownie;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.file.OpenOptions;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import jp.skypencil.brownie.fs.DistributedFileSystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class FileEncoder {
    private static final String TEMP_DIR = System.getenv("java.io.tmpdir");
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private Vertx vertx;

    @Resource
    private DistributedFileSystem fileSystem;

    @PostConstruct
    public void listenEvent() {
        vertx.eventBus().localConsumer("file-uploaded", (Message<Task> message) -> {
            Task task = message.body();
            download(task.getKey(), downloaded -> {
                if (downloaded.failed()) {
                    logger.error("Failed to download file (key: {}) from distributed file system", task.getKey(), downloaded.cause());
                    message.fail(1, "Failed to download file from distributed file system");
                    return;
                }
                File downloadedFile = downloaded.result();
                logger.debug("Downloaded file (key: {}) to {}",
                        task.getKey(),
                        downloadedFile);
                for (String resolution : task.getResolutions()) {
                    convertToAllResolution(downloadedFile, resolution, converted -> {
                        if (converted.failed()) {
                            logger.error("Failed to convert file (key: {}, resolution: {})",
                                    task.getKey(),
                                    resolution,
                                    converted.cause());
                            // TODO do we need to call message.fail() even when file is invalid?
                            // TODO how to handle only a part of resolutions have problem?
                            message.fail(2, "Failed to convert file");
                            return;
                        }
                        String resultFileName = converted.result().toString();
                        logger.info("Converted file (key: {}, resolution: {}) to {}",
                                task.getKey(),
                                resolution,
                                resultFileName);
                    });
                }
            });
        });
    }

    private void download(UUID key, Handler<AsyncResult<File>> handler) {
        String downloadedFile = TEMP_DIR + "/" + new com.eaio.uuid.UUID();
        Future<File> future = Future.future();
        vertx.fileSystem().open(downloadedFile, new OpenOptions().setWrite(true), result -> {
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

    private void convertToAllResolution(File targetFile, String resolution, Handler<AsyncResult<Object>> handler) {
        final int processors = Runtime.getRuntime().availableProcessors();
        vertx.executeBlocking(
                convert(targetFile, resolution, processors),
                true,
                handler);
    }

    private Handler<Future<Object>> convert(File targetFile,
            String resolution, final int processors) {
        return future -> {
            logger.info("Converting {} to {}", targetFile, resolution);
            String resultFileName = targetFile.getAbsolutePath() + "-" + resolution + ".webm";
            ProcessBuilder builder = new ProcessBuilder().command("ffmpeg",
                    "-i", targetFile.getAbsolutePath(),
                    "-s", resolution,
                    // Vorbis encoder only supports 2 channels. http://stackoverflow.com/a/19005961
                    "-ac", "2",
                    // 'vorbis' is now experimental, we need following parameter to enable it.
                    "-strict", "-2",
                    "-n",
                    "-threads", Integer.toString(processors * 3 / 2),
                    resultFileName);
            builder.redirectError();
            builder.redirectOutput();
            try {
                Process process = builder.start();
                int statusCode;
                if ((statusCode = process.waitFor()) != 0) {
                    throw new IllegalStateException("FFmpeg failed with illegal status code: " + statusCode);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
            future.complete(resultFileName);
        };
    }
}
