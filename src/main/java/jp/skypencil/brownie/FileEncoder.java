package jp.skypencil.brownie;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import jp.skypencil.brownie.fs.DistributedFileSystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class FileEncoder {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private Vertx vertx;

    @Resource
    private DistributedFileSystem fileSystem;

    @PostConstruct
    public void listenEvent() {
        vertx.eventBus().localConsumer("file-uploaded", (Message<Task> message) -> {
            Task task = message.body();
            fileSystem.load(task.getKey(), loaded -> {
                if (loaded.failed()) {
                    throw new RuntimeException("Failed to load file from distribtued file system", loaded.cause());
                }
                try {
                    File downloadedFile = Files.createTempFile("brownie-downloaded", ".video").toFile();
                    vertx.fileSystem().writeFile(downloadedFile.getAbsolutePath(), loaded.result(), written -> {
                        if (written.failed()) {
                            throw new RuntimeException("Failed to store file onto local file system", loaded.cause());
                        }
                        logger.debug("received {}", downloadedFile);
                        for (String resolution : task.getResolutions()) {
                            convertToAllResolution(downloadedFile, resolution);
                        }
                    });
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        });
    }

    private void convertToAllResolution(File targetFile, String resolution) {
        final int processors = Runtime.getRuntime().availableProcessors();
        vertx.executeBlocking(
                convert(targetFile, resolution, processors),
                true,
                handleResult());
    }

    private Handler<AsyncResult<Object>> handleResult() {
        return result -> {
            if (result.failed()) {
                throw new RuntimeException("Failed to convert file", result.cause());
            }
            String resultFileName = result.result().toString();
            logger.info("Converted to {}", resultFileName);
        };
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
