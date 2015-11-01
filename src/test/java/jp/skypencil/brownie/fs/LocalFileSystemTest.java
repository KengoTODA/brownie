package jp.skypencil.brownie.fs;

import static com.google.common.truth.Truth.assertThat;
import io.vertx.core.buffer.Buffer;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jp.skypencil.brownie.VertxResource;

import org.junit.Rule;
import org.junit.Test;

public class LocalFileSystemTest {
    @Rule
    public VertxResource vertxResource = new VertxResource();

    @Test
    public void test() throws InterruptedException {
        LocalFileSystem fileSystem = new LocalFileSystem();
        fileSystem.vertx = vertxResource.getVertx();
        UUID key = UUID.randomUUID();
        byte[] initialData = new byte[128];
        new Random().nextBytes(initialData);

        Buffer buffer = Buffer.buffer(initialData);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<byte[]> loaded = new AtomicReference<>();

        fileSystem.store(key, buffer, stored -> {
            assertThat(stored.failed()).isFalse();
            fileSystem.load(key, (result) -> {
                assertThat(result.failed()).isFalse();
                loaded.set(result.result().getBytes());
                latch.countDown();
            });
        });

        latch.await(100, TimeUnit.MILLISECONDS);
        assertThat(loaded.get())
            .named("loaded data")
            .isEqualTo(initialData);
    }

}
