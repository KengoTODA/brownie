package jp.skypencil.brownie;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import jp.skypencil.brownie.fs.SharedFileSystem;
import jp.skypencil.brownie.registry.FileMetadataRegistry;
import rx.Observable;
import rx.Single;

@RunWith(VertxUnitRunner.class)
public class FileTransporterTest {

    private Vertx vertx;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
    }

    @After
    public void cleanUp(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testUpload(TestContext context) throws IOException {
        Async async = context.async();
        SharedFileSystem fileSystem = Mockito.mock(SharedFileSystem.class);
        FileMetadataRegistry fileMetadataRegistry = Mockito.mock(FileMetadataRegistry.class);
        FileTransporter transporter = new FileTransporter(vertx, fileSystem, fileMetadataRegistry); 

        UUID id = UUID.randomUUID();
        File file = folder.newFile();
        doReturn(Single.just((Void) null)).when(fileSystem).store(eq(id), any());
        doReturn(Single.just((Void) null)).when(fileMetadataRegistry).store(any());
        transporter.upload(id, "name", file, MimeType.valueOf("text/plain")).subscribe(v -> {
            Mockito.verify(fileSystem).store(eq(id), any());
            Mockito.verify(fileMetadataRegistry).store(any());
            async.complete();
        });
    }

    @Test
    public void testDelete(TestContext context) throws IOException {
        Async async = context.async();
        SharedFileSystem fileSystem = Mockito.mock(SharedFileSystem.class);
        FileMetadataRegistry fileMetadataRegistry = Mockito.mock(FileMetadataRegistry.class);
        FileTransporter transporter = new FileTransporter(vertx, fileSystem, fileMetadataRegistry); 

        UUID id = UUID.randomUUID();
        doReturn(Single.just((Void) null)).when(fileSystem).delete(eq(id));
        doReturn(Single.just((Void) null)).when(fileMetadataRegistry).delete(eq(id));
        transporter.delete(id).subscribe(v -> {
            Mockito.verify(fileSystem).delete(eq(id));
            Mockito.verify(fileMetadataRegistry).delete(eq(id));
            async.complete();
        });
    }

    @Test
    public void testDownload(TestContext context) throws IOException {
        Async async = context.async();
        SharedFileSystem fileSystem = Mockito.mock(SharedFileSystem.class);
        FileMetadataRegistry fileMetadataRegistry = Mockito.mock(FileMetadataRegistry.class);
        FileTransporter transporter = new FileTransporter(vertx, fileSystem, fileMetadataRegistry); 

        UUID id = UUID.randomUUID();
        doReturn(Observable.just(Buffer.buffer("buffer"))).when(fileSystem).load(eq(id));
        transporter.download(id).subscribe(v -> {
            Mockito.verify(fileSystem).load(eq(id));
            async.complete();
        });
    }
}
