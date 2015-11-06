package jp.skypencil.brownie;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import java.io.File;
import java.io.IOException;

import javax.annotation.Resource;

import jp.skypencil.brownie.fs.DistributedFileSystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class FileEncoderFFmpeg implements FileEncoder {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private Vertx vertx;

    @Resource
    private DistributedFileSystem fileSystem;

    @Override
    public void convert(File targetFile, String resolution, Handler<AsyncResult<File>> handler) {
        final int processors = Runtime.getRuntime().availableProcessors();
        vertx.executeBlocking(
                convert(targetFile, resolution, processors),
                true,
                handler);
    }

    private Handler<Future<File>> convert(File targetFile,
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
                    future.fail("FFmpeg failed with illegal status code: " + statusCode);
                    return;
                }
            } catch (IOException | InterruptedException e) {
                future.fail(e);
                return;
            }
            future.complete(new File(resultFileName));
        };
    }
}
