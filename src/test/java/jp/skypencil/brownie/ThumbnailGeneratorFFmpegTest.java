package jp.skypencil.brownie;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assume.assumeTrue;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.Vertx;

@RunWith(VertxUnitRunner.class)
public class ThumbnailGeneratorFFmpegTest {
    private ThumbnailGeneratorFFmpeg generator;
    private Vertx vertx;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        generator = new ThumbnailGeneratorFFmpeg(vertx);
    }

    @After
    public void cleanUp(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testGenerate(TestContext context) {
        File video = new File("src/test/resources", "ThumbnailGeneratorFFmpegTest.webm");
        assumeTrue(video.exists());

        Async async = context.async();
        generator.generate(video, 3_000).subscribe(converted -> {
            context.assertTrue(converted.exists());
            context.assertTrue(converted.getName().endsWith(".jpg"));
            context.assertTrue(converted.length() > 0);
            async.complete();
        }, context::fail);
    }

    @Test
    public void testFormat() {
        assertThat(generator.format(0)).isEqualTo("00:00:00.000");
        assertThat(generator.format(123_456_789)).isEqualTo("34:17:36.789");
    }

}
