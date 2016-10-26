package jp.skypencil.brownie;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
import jp.skypencil.brownie.event.VideoUploadedEvent;

@RunWith(VertxUnitRunner.class)
public class FrontendServerTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Vertx vertx;

    @Before
    public void setUp() {
        io.vertx.core.Vertx normalVertx = io.vertx.core.Vertx.vertx();
        normalVertx.eventBus().registerDefaultCodec(VideoUploadedEvent.class, new VideoUploadedEventCodec());

        vertx = Vertx.newInstance(normalVertx);
    }

    @After
    public void cleanUp(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testHandleFormWithNoUploadedFile(TestContext context) {
        FrontendServer server = new FrontendServer(ServiceDiscovery.create(vertx));

        HttpServerResponse response = mock(HttpServerResponse.class);
        RoutingContext ctx = mock(RoutingContext.class);
        doReturn(response).when(ctx).response();
        doReturn(response).when(response).putHeader(anyString(), anyString());
        doReturn(Collections.emptySet()).when(ctx).fileUploads();

        server.handleForm(ctx);
        verify(response).end(eq("No file uploaded"));
    }
//
//    @Test
//    public void testHandleFormWithOneUploadedFile(TestContext context) throws IOException {
//        FileTransporter fileTransporter = mock(FileTransporter.class);
//        VideoUploadedEventRegistry taskRegistry = mock(VideoUploadedEventRegistry.class);
//        Future<Void> uploadedFuture = Future.future();
//        doReturn(uploadedFuture.setHandlerObservable().toSingle()).when(fileTransporter)
//                .upload(any(UUID.class), anyString(), any(File.class), eq(MimeType.valueOf("text/plain")));
//        Future<Void> storedFuture = Future.future();
//        doReturn(storedFuture.setHandlerObservable().toSingle()).when(taskRegistry).store(any(VideoUploadedEvent.class));
//        FrontendServer server = new FrontendServer(
//                fileTransporter,
//                taskRegistry,
//                mock(FileMetadataRegistry.class),
//                mock(ThumbnailMetadataRegistry.class),
//                new IdGenerator());
//        server.init((io.vertx.core.Vertx) vertx.getDelegate(), null);
//        FileUpload fileUpload = mock(FileUpload.class);
//        doReturn("text/plain").when(fileUpload).contentType();
//        doReturn(folder.newFile().getAbsolutePath()).when(fileUpload).uploadedFileName();
//        doReturn("fileName").when(fileUpload).fileName();
//
//        HttpServerResponse response = mock(HttpServerResponse.class);
//        RoutingContext ctx = mock(RoutingContext.class);
//        doReturn(response).when(ctx).response();
//        doReturn(response).when(response).putHeader(anyString(), anyString());
//        doReturn(Collections.singleton(fileUpload)).when(ctx).fileUploads();
//
//        server.handleForm(ctx);
//        uploadedFuture.complete();
//        storedFuture.complete();
//        verify(response, never()).setStatusCode(eq(500));
//        verify(response, never()).end(eq("Internal server error"));
//        verify(response).end(eq("registered"));
//    }
//
//    @Test
//    public void testHandleFormWithTaskRegistryError(TestContext context) throws IOException {
//        FileTransporter fileTransporter = mock(FileTransporter.class);
//        VideoUploadedEventRegistry taskRegistry = mock(VideoUploadedEventRegistry.class);
//        Future<Void> uploadedFuture = Future.future();
//        doReturn(uploadedFuture.setHandlerObservable().toSingle()).when(fileTransporter)
//                .upload(any(UUID.class), anyString(), any(File.class), eq(MimeType.valueOf("text/plain")));
//        Future<Void> storedFuture = Future.future();
//        doReturn(storedFuture.setHandlerObservable().toSingle()).when(taskRegistry).store(any(VideoUploadedEvent.class));
//        FrontendServer server = new FrontendServer(
//                fileTransporter,
//                taskRegistry,
//                mock(FileMetadataRegistry.class),
//                mock(ThumbnailMetadataRegistry.class),
//                new IdGenerator());
//        server.init((io.vertx.core.Vertx) vertx.getDelegate(), null);
//        FileUpload fileUpload = mock(FileUpload.class);
//        doReturn("text/plain").when(fileUpload).contentType();
//        doReturn(folder.newFile().getAbsolutePath()).when(fileUpload).uploadedFileName();
//        doReturn("fileName").when(fileUpload).fileName();
//
//        HttpServerResponse response = mock(HttpServerResponse.class);
//        RoutingContext ctx = mock(RoutingContext.class);
//        doReturn(response).when(ctx).response();
//        doReturn(response).when(response).setStatusCode(eq(500));
//        doReturn(response).when(response).putHeader(anyString(), anyString());
//        doReturn(Collections.singleton(fileUpload)).when(ctx).fileUploads();
//
//        server.handleForm(ctx);
//        uploadedFuture.complete();
//        storedFuture.fail(new RuntimeException("fail"));
//        verify(response).setStatusCode(eq(500));
//        verify(response).end(eq("Internal server error"));
//        verify(response, never()).end(eq("registered"));
//    }
//
//    @Test
//    public void testHandleFormWithFileTransporterError(TestContext context) throws IOException {
//        FileTransporter fileTransporter = mock(FileTransporter.class);
//        VideoUploadedEventRegistry taskRegistry = mock(VideoUploadedEventRegistry.class);
//        Future<Void> uploadedFuture = Future.future();
//        doReturn(uploadedFuture.setHandlerObservable().toSingle()).when(fileTransporter)
//                .upload(any(UUID.class), anyString(), any(File.class), eq(MimeType.valueOf("text/plain")));
//        Future<Void> storedFuture = Future.future();
//        doReturn(storedFuture.setHandlerObservable().toSingle()).when(taskRegistry).store(any(VideoUploadedEvent.class));
//        FrontendServer server = new FrontendServer(
//                fileTransporter,
//                taskRegistry,
//                mock(FileMetadataRegistry.class),
//                mock(ThumbnailMetadataRegistry.class),
//                new IdGenerator());
//        server.init((io.vertx.core.Vertx) vertx.getDelegate(), null);
//        FileUpload fileUpload = mock(FileUpload.class);
//        doReturn("text/plain").when(fileUpload).contentType();
//        doReturn(folder.newFile().getAbsolutePath()).when(fileUpload).uploadedFileName();
//        doReturn("fileName").when(fileUpload).fileName();
//
//        HttpServerResponse response = mock(HttpServerResponse.class);
//        RoutingContext ctx = mock(RoutingContext.class);
//        doReturn(response).when(ctx).response();
//        doReturn(response).when(response).setStatusCode(eq(500));
//        doReturn(response).when(response).putHeader(anyString(), anyString());
//        doReturn(Collections.singleton(fileUpload)).when(ctx).fileUploads();
//
//        server.handleForm(ctx);
//        uploadedFuture.fail(new RuntimeException("fail"));
//        storedFuture.complete();
//        verify(response).setStatusCode(eq(500));
//        verify(response).end(eq("Internal server error"));
//        verify(response, never()).end(eq("registered"));
//    }
}
