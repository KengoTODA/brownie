package jp.skypencil.brownie.registry;

import java.time.Instant;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.MimeType;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import jp.skypencil.brownie.FileId;
import jp.skypencil.brownie.FileMetadata;

public abstract class FileMetadataRegistryTest {
    Vertx vertx;

    @Nonnull
    abstract FileMetadataRegistry createRegistry();

    @Before
    public final void setUp() {
        vertx = Vertx.vertx();
    }

    @After
    public final void cleanUp(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testDelete(TestContext context) throws InterruptedException {
        FileMetadataRegistry registry = createRegistry();
        Async async = context.async();
        @FileId
        UUID fileId = UUID.randomUUID();
        FileMetadata metadata = new FileMetadata(fileId, "fileName",
                MimeType.valueOf("text/plain"), 0, Instant.now());
        registry.store(metadata, stored -> {
            context.assertTrue(stored.succeeded(), "" + stored.cause());
            registry.load(fileId, loaded -> {
                context.assertTrue(loaded.succeeded(), "" + loaded.cause());
                context.assertEquals(fileId, loaded.result().get().getFileId());
                registry.delete(fileId, deleted -> {
                    context.assertTrue(deleted.succeeded());
                    async.complete();
                });
            });
        });
    }

    @Test
    public void testDeleteFailedWhenWeHaveNoFileMetadata(TestContext context) throws InterruptedException {
        FileMetadataRegistry registry = createRegistry();
        Async async = context.async();
        @FileId
        UUID fileId = UUID.randomUUID();
        registry.delete(fileId, deleted -> {
            context.assertTrue(deleted.failed());
            async.complete();
        });
    }

    @Test
    public void testIterate(TestContext context) {
        FileMetadataRegistry registry = createRegistry();
        Async async = context.async();
        @FileId
        UUID fileId = UUID.randomUUID();
        FileMetadata metadata = new FileMetadata(fileId, "fileName",
                MimeType.valueOf("text/plain"), 0, Instant.now());
        registry.store(metadata, stored -> {
            context.assertTrue(stored.succeeded());
            registry.iterate(iterated -> {
                context.assertTrue(iterated.succeeded());
                iterated.result().endHandler(ended -> {
                    async.complete();
                }).handler(handled -> {
                });
            });
        });
    }
}
