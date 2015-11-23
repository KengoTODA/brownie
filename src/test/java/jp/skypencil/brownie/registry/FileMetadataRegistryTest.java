package jp.skypencil.brownie.registry;

import static com.google.common.truth.Truth.assertThat;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jp.skypencil.brownie.FileId;
import jp.skypencil.brownie.FileMetadata;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.util.MimeType;

@RunWith(Parameterized.class)
public class FileMetadataRegistryTest {
    @Parameters
    public static Object[][] createImplementations() {
        return new Object[][]{
                new Object[] {
                        new FileMetadataRegistryOnMemory()
                }
        };
    }

    @Parameter(0)
    public FileMetadataRegistry registry;

    @Test
    public void testDelete() throws InterruptedException {
        @FileId
        UUID fileId = UUID.randomUUID();
        CountDownLatch latch = new CountDownLatch(1);
        FileMetadata metadata = new FileMetadata(fileId, "fileName",
                MimeType.valueOf("text/plain"), 0, Instant.now());
        registry.store(metadata, stored -> {
            registry.delete(fileId, deleted -> {
                assertThat(deleted.succeeded()).isTrue();
                latch.countDown();
            });
        });
        assertThat(latch.await(100, TimeUnit.MILLISECONDS)).isTrue();
    }

    @Test
    public void testDeleteFailedWhenWeHaveNoFileMetadata() throws InterruptedException {
        @FileId
        UUID fileId = UUID.randomUUID();
        CountDownLatch latch = new CountDownLatch(1);
        registry.delete(fileId, deleted -> {
            assertThat(deleted.failed()).isTrue();
            latch.countDown();
        });
        assertThat(latch.await(100, TimeUnit.MILLISECONDS)).isTrue();
    }
}
