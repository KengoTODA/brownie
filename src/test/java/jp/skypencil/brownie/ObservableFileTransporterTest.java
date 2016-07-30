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
import org.springframework.util.MimeType;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import jp.skypencil.brownie.fs.ObservableSharedFileSystem;
import jp.skypencil.brownie.registry.ObservableFileMetadataRegistry;
import rx.Observable;

@RunWith(VertxUnitRunner.class)
public class ObservableFileTransporterTest {

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
        ObservableSharedFileSystem fileSystem = Mockito.mock(ObservableSharedFileSystem.class);
        ObservableFileMetadataRegistry fileMetadataRegistry = Mockito.mock(ObservableFileMetadataRegistry.class);
        ObservableFileTransporter transporter = new ObservableFileTransporter(vertx, fileSystem, fileMetadataRegistry); 

        UUID key = UUID.randomUUID();
        File file = folder.newFile();
        doReturn(Observable.just((Void) null)).when(fileSystem).store(eq(key), any());
        doReturn(Observable.just((Void) null)).when(fileMetadataRegistry).store(any());
        transporter.upload(key, "name", file, MimeType.valueOf("text/plain")).subscribe(v -> {
            Mockito.verify(fileSystem).store(eq(key), any());
            Mockito.verify(fileMetadataRegistry).store(any());
            async.complete();
        });
    }

    @Test
    public void testDelete(TestContext context) throws IOException {
        Async async = context.async();
        ObservableSharedFileSystem fileSystem = Mockito.mock(ObservableSharedFileSystem.class);
        ObservableFileMetadataRegistry fileMetadataRegistry = Mockito.mock(ObservableFileMetadataRegistry.class);
        ObservableFileTransporter transporter = new ObservableFileTransporter(vertx, fileSystem, fileMetadataRegistry); 

        UUID key = UUID.randomUUID();
        doReturn(Observable.just((Void) null)).when(fileSystem).delete(eq(key));
        doReturn(Observable.just((Void) null)).when(fileMetadataRegistry).delete(eq(key));
        transporter.delete(key).subscribe(v -> {
            Mockito.verify(fileSystem).delete(eq(key));
            Mockito.verify(fileMetadataRegistry).delete(eq(key));
            async.complete();
        });
    }

    @Test
    public void testDownload(TestContext context) throws IOException {
        Async async = context.async();
        ObservableSharedFileSystem fileSystem = Mockito.mock(ObservableSharedFileSystem.class);
        ObservableFileMetadataRegistry fileMetadataRegistry = Mockito.mock(ObservableFileMetadataRegistry.class);
        ObservableFileTransporter transporter = new ObservableFileTransporter(vertx, fileSystem, fileMetadataRegistry); 

        UUID key = UUID.randomUUID();
        doReturn(Observable.just(Buffer.buffer("buffer"))).when(fileSystem).load(eq(key));
        transporter.download(key).subscribe(v -> {
            Mockito.verify(fileSystem).load(eq(key));
            async.complete();
        });
    }
}
