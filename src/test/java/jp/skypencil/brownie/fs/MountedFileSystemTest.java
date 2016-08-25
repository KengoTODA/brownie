package jp.skypencil.brownie.fs;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import io.vertx.core.file.FileSystemException;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;

@RunWith(VertxUnitRunner.class)
public class MountedFileSystemTest {
    private Vertx vertx;
    private MountedFileSystem fileSystem;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public final void setUp() throws IOException {
        vertx = Vertx.vertx();
        fileSystem = new MountedFileSystem(folder.newFolder().getAbsolutePath(), vertx);
    }

    @After
    public final void cleanUp(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testStore(TestContext context) {
        Async async = context.async();
        UUID id = UUID.randomUUID();
        Buffer buffer = Buffer.buffer("buffer");
        fileSystem.store(id, buffer).subscribe(onNext -> {
            context.assertNull(onNext);
        }, context::fail, () -> {
            async.complete();
        });
    }

    @Test
    public void testLoadFileWhichDoesNotExist(TestContext context) {
        Async async = context.async();
        UUID id = UUID.randomUUID();
        fileSystem.load(id).subscribe(onNext -> {
            context.fail();
        }, error -> {
            context.assertTrue(error instanceof FileSystemException);
            context.assertTrue(error.getCause() instanceof NoSuchFileException);
            async.complete();
        }, () -> {
            context.fail();
        });
    }

    @Test
    public void testLoad(TestContext context) {
        Async async = context.async();
        UUID id = UUID.randomUUID();
        Buffer buffer = Buffer.buffer("buffer");
        fileSystem.store(id, buffer).flatMap(v -> {
            return fileSystem.load(id);
        }).subscribe(onNext -> {
            context.assertEquals("buffer", onNext.toString());
        }, context::fail, () -> {
            async.complete();
        });
    }

    @Test
    public void testDelete(TestContext context) {
        Async async = context.async();
        UUID id = UUID.randomUUID();
        Buffer buffer = Buffer.buffer("buffer");
        fileSystem.store(id, buffer).flatMap(v -> {
            return fileSystem.delete(id);
        }).flatMap(v -> {
            return fileSystem.load(id);
        }).subscribe(onNext -> {
            context.fail();
        }, error -> {
            context.assertTrue(error instanceof FileSystemException);
            context.assertTrue(error.getCause() instanceof NoSuchFileException);
            async.complete();
        }, () -> {
            context.fail();
        });
    }

}
