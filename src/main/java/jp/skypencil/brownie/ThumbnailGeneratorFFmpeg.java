package jp.skypencil.brownie;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnegative;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import rx.Single;

@Component
@Slf4j
@ParametersAreNonnullByDefault
@RequiredArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE) // for unit test
class ThumbnailGeneratorFFmpeg implements ThumbnailGenerator {
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    @Resource
    private Vertx rxJavaVertx;

    @Override
    public Single<File> generate(File video, int milliseconds) {
        return rxJavaVertx.executeBlockingObservable(generateInternal(video, milliseconds)).toSingle();
    }

    private Handler<Future<File>> generateInternal(File video, 
            @Nonnegative int milliseconds) {
        File thumbnail = new File(TEMP_DIR, new com.eaio.uuid.UUID() + ".jpg");
        return future -> {
            log.info("Start generating thumbnail of {}...", video.getAbsolutePath());
            // https://trac.ffmpeg.org/wiki/Create%20a%20thumbnail%20image%20every%20X%20seconds%20of%20the%20video
            ProcessBuilder builder = new ProcessBuilder().command("ffmpeg",
                    "-i", video.getAbsolutePath(),
                    "-ss", format(milliseconds),
                    "-vframes", "1",
                    thumbnail.getAbsolutePath());
            builder.redirectError();
            builder.redirectOutput();
            log.debug("Executing ffmpeg command: {}", builder.command());
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
            log.info("Generated thumbnail of {} to {}", video.getAbsolutePath(), thumbnail.getAbsolutePath());
            future.complete(thumbnail);
        };
    }

    String format(@Nonnegative int milliseconds) {
        if (milliseconds < 0) {
            throw new IllegalArgumentException("milliseconds should be positive number, but it was " + milliseconds);
        }

        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60;
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds % 1_000);
    }
}
