package jp.skypencil.brownie;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpHeaders;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.eventbus.Message;
import io.vertx.rxjava.core.eventbus.MessageConsumer;
import io.vertx.rxjava.core.file.AsyncFile;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpClientResponse;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
import jp.skypencil.brownie.event.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.Single;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class EncodeServer extends AbstractVerticle {
    private final FileEncoder fileEncoder;

    private final IdGenerator idGenerator;

    private final ServiceDiscovery discovery;

    private final String directory = createDirectory();

    @Override
    public void start() throws Exception {
        registerEventListeners();
    }

    @Nonnull
    private String createDirectory() {
        try {
            Path directory = Files.createTempDirectory("brownie");
            log.debug("Directory to store file is created at {}", directory);
            return directory.toFile().getAbsolutePath();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Single<File> saveFileToLocal(UUID fileId) {
        UUID localFileId = idGenerator.generateUuidV1();
        File localFile = new File(directory, localFileId.toString());
        return vertx.fileSystem()
            .openObservable(localFile.getAbsolutePath(), new OpenOptions().setCreateNew(true))
            .toSingle()
            .flatMap(asyncFile -> {
                return downloadFileFromFileStorage(fileId)
                    .reduce(asyncFile, (v, buffer) -> {
                        return asyncFile.write(buffer);
                    })
                    .toSingle()
                    .doAfterTerminate(asyncFile::close);
            })
            .map(asyncFile -> {
                return localFile;
            });
        
    }

    private Observable<Buffer> downloadFileFromFileStorage(UUID videoId) {
        return createHttpClientForFileStorage()
                .toObservable()
                .flatMap(client -> {
                    HttpClientRequest req = client.get("/file/" + videoId);
                    Observable<HttpClientResponse> result = req.toObservable();
                    req.end();
                    return result;
                })
                .flatMap(HttpClientResponse::toObservable);
    }

    private void registerEventListeners() {
        MessageConsumer<VideoUploadedEvent> consumer = vertx.eventBus().localConsumer("file-uploaded");
        consumer.toObservable().subscribe(message -> {
            VideoUploadedEvent task = message.body();
            saveFileToLocal(task.getId()).doOnSuccess(downloadedFile -> {
                log.debug("Downloaded file (id: {}) to {}",
                        task.getId(),
                        downloadedFile);
            }).doOnError(error -> {
                log.error("Failed to download file (id: {}) from distributed file system", task.getId(), error);
                message.fail(1, "Failed to download file from distributed file system");
            }).toObservable().flatMap(downloadedFile -> {
                return Observable.from(task.getResolutions()).flatMap(resolution -> {
                    return convert(downloadedFile, resolution, message).toObservable();
                });
            }).flatMap(convertedFile -> {
                return upload(convertedFile, task.getUploadedFileName(), message).toObservable();
            }).subscribe();
        });
    }

    private Single<File> convert(File source, String resolution, Message<VideoUploadedEvent> message) {
        return fileEncoder.convert(source, resolution)
                .doOnSuccess(converted -> {
                    log.info("Converted file (path: {}, resolution: {}) to {}", new Object[]{
                            source.getAbsolutePath(),
                            resolution,
                            converted.getAbsoluteFile()});
                }).doOnError(error -> {
                    log.error("Failed to convert file (path: {}, resolution: {})", new Object[]{
                            source.getAbsolutePath(),
                            resolution,
                            error});
                    // TODO do we need to call message.fail() even when file is invalid?
                    // TODO how to handle only a part of resolutions have problem?
                    message.fail(2, "Failed to convert file");
                });
    }

    private Single<Void> upload(File source, String fileName,
            Message<VideoUploadedEvent> message) {
        UUID id = idGenerator.generateUuidV1(); 
        return createHttpClientForFileStorage()
            .flatMap(client -> {
                HttpClientRequest req = client.post("/file/")
                    .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "video/mpeg");
                Single<HttpClientResponse> result = req.toObservable().toSingle();
                return Single.zip(result, writeTo(req, source), (res, v) -> {
                    return (HttpClientResponse) res;
                });
            })
            .map(res -> {
                if (res.statusCode() != 200) {
                    log.error("Failed to store file ({})", source.getAbsolutePath());
                    throw new RuntimeException("Failed to store video ("
                            + source.getAbsolutePath()
                            + ") to file storage. Status code is: "
                            + res.statusCode() + ", status message is: "
                            + res.statusMessage());
                }
                log.info("Uploaded file ({})", source.getAbsolutePath());
                vertx.eventBus().send("generate-thumbnail", id);
                return (Void) null;
            });
    }

    private Single<Void> writeTo(HttpClientRequest req, File source) {
        return vertx.fileSystem()
            .openObservable(source.getAbsolutePath(),
                    new OpenOptions().setCreate(false).setWrite(false).setRead(true))
            .flatMap(AsyncFile::toObservable)
            .collect(() -> req, HttpClientRequest::write)
            .toSingle()
            .map(client -> {
                client.end();
                return null;
            });
    }

    private Single<HttpClient> createHttpClientForFileStorage() {
        Single<HttpClient> clientSingle = discovery.getRecordObservable(r -> r.getName().equals("file-storage"))
            .map(discovery::getReference)
            .flatMap(reference -> {
                HttpClient client = reference.get();
                return Observable.just(client)
                        .doAfterTerminate(client::close)
                        .doAfterTerminate(reference::release);
            })
            .toSingle();
        return clientSingle;
    }
}
