package jp.skypencil.brownie;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class FileEncoder {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private Vertx vertx;

    @Resource
    public void setVertx(Vertx vertx) {
        this.vertx = vertx;
    }

    @PostConstruct
    public void listenEvent() {
        vertx.eventBus().localConsumer("file-uploaded", (Message<Task> message) -> {
            Task task = message.body();
            File uploadedFile = new File(task.getUploadedFileName());
            logger.debug("received {}", uploadedFile);

            for (String resolution : task.getResolutions()) {
                convertToAllResolution(task.getUploadedFileName(), resolution);
            }
        });
    }

    private void convertToAllResolution(String targetFilePath, String resolution) {
        final int processors = Runtime.getRuntime().availableProcessors();
        vertx.executeBlocking(
                convert(targetFilePath, resolution, processors),
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

    private Handler<Future<Object>> convert(String targetFilePath,
            String resolution, final int processors) {
        return future -> {
            logger.info("Converting {} to {}", targetFilePath, resolution);
            String resultFileName = targetFilePath + "-" + resolution + ".webm";
            ProcessBuilder builder = new ProcessBuilder().command("ffmpeg",
                    "-i", targetFilePath,
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
