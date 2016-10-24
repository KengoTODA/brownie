package jp.skypencil.brownie.fs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import com.google.common.io.Files;

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
            context.assertEquals(id, onNext);
            async.complete();
        }, context::fail);
    }

    @Test
    public void testLoadFileWhichDoesNotExist(TestContext context) {
        Async async = context.async();
        UUID id = UUID.randomUUID();
        fileSystem.load(id).subscribe(onNext -> {
            context.fail();
        }, error -> {
            context.assertTrue(error instanceof IllegalArgumentException);
            async.complete();
        });
    }

    @Test
    public void testLoad(TestContext context) {
        Async async = context.async();
        UUID id = UUID.randomUUID();
        Buffer buffer = Buffer.buffer("buffer");
        fileSystem.store(id, buffer).toObservable().flatMap(v -> {
            return fileSystem.load(id).toObservable();
        })
        .subscribe(file -> {
            try {
                context.assertEquals("buffer", Files.toString(file, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
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
        }).toObservable().flatMap(v -> {
            return fileSystem.load(id).toObservable();
        }).subscribe(onNext -> {
            context.fail();
        }, error -> {
            context.assertTrue(error instanceof IllegalArgumentException);
            async.complete();
        }, () -> {
            context.fail();
        });
    }

}
