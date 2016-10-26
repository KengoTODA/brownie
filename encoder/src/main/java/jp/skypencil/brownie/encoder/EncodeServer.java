package jp.skypencil.brownie.encoder;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import io.vertx.core.Future;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpHeaders;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.eventbus.Message;
import io.vertx.rxjava.core.eventbus.MessageConsumer;
import io.vertx.rxjava.core.file.AsyncFile;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpClientResponse;
import io.vertx.rxjava.core.streams.Pump;
import io.vertx.rxjava.core.streams.ReadStream;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
import jp.skypencil.brownie.IdGenerator;
import jp.skypencil.brownie.event.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.Single;
import scala.Tuple2;

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
        Future<Void> httpClosed = Future.future();
        Single<ReadStream<Buffer>> read = downloadFileFromFileStorage(fileId, httpClosed);
        Single<AsyncFile> write = vertx.fileSystem()
            .openObservable(localFile.getAbsolutePath(), new OpenOptions().setCreateNew(true))
            .toSingle();

        return Single.create(subscriber -> {
            read.zipWith(write, (readStream, writeStream) -> {
                readStream.exceptionHandler(subscriber::onError);
                writeStream.exceptionHandler(subscriber::onError);
                readStream.endHandler(v -> {
                    httpClosed.complete(v);
                    writeStream.close(closed -> {
                        log.info("Saved data to local file. Its size is {}", localFile.length());
                        subscriber.onSuccess(localFile);
                    });
                });
                return Pump.pump(readStream, writeStream).start();
            }).subscribe(v -> {}, subscriber::onError);
        });
    }

    private Single<ReadStream<Buffer>> downloadFileFromFileStorage(UUID videoId, Future<Void> closed) {
        return createHttpClientForFileStorage(closed)
                .flatMap(client -> {
                    // FIXME id might be invalid
                    return RxHelper.get(client, "/file/" + videoId).toSingle();
                })
                .map(clientRes -> {
                    if (clientRes.statusCode() != 200) {
                        closed.complete();
                        throw new RuntimeException("Status code is not 200 but " + clientRes.statusCode());
                    }
                    return clientRes;
                });
    }

    private void registerEventListeners() {
        MessageConsumer<VideoUploadedEvent> consumer = vertx.eventBus().consumer("file-uploaded");
        consumer.toObservable().flatMap(message -> {
            VideoUploadedEvent event = message.body();
            return (Observable<Tuple2<File, Message<VideoUploadedEvent>>>)
                    saveFileToLocal(event.getId()).zipWith(Single.just(message), Tuple2::apply).toObservable();
        }).flatMap(tuple -> {
            File downloadedFile = tuple._1;
            Message<VideoUploadedEvent> message = tuple._2;
            VideoUploadedEvent event = message.body();
            return (Observable<Tuple2<File, Message<VideoUploadedEvent>>>) Observable.from(event.getResolutions()).flatMap(resolution -> {
                return convert(downloadedFile, resolution, message).toObservable();
            }).zipWith(Observable.just(message).repeat(), Tuple2::apply);
        }).flatMap(tuple -> {
            File convertedFile = tuple._1;
            Message<VideoUploadedEvent> message = tuple._2;
            VideoUploadedEvent event = message.body();
            log.info("Conversion finished, now we are going to upload video file");
            return upload(convertedFile, event.getUploadedFileName(), message).toObservable();
        }).subscribe();
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
        UUID videoId = message.body().getId();
        Future<Void> closed = Future.future();
        return createHttpClientForFileStorage(closed)
            .flatMap(client -> {
                HttpClientRequest req = client.post("/file/")
                    .setChunked(true)
                    .putHeader(HttpHeaders.CONTENT_LENGTH.toString(), Long.toString(source.length()))
                    .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "video/webm")
                    .putHeader("File-Name", message.body().getUploadedFileName());
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
                vertx.eventBus().send("generate-thumbnail", videoId);
                return (Void) null;
            })
            .doAfterTerminate(closed::complete);
    }

    private Single<Void> writeTo(HttpClientRequest req, File source) {
        return vertx.fileSystem()
                .openObservable(source.getAbsolutePath(),
                        new OpenOptions().setCreate(false).setWrite(false).setRead(true))
                .toSingle()
                .flatMap(readStream -> {
                    return Single.create(subscriber -> {
                        req.exceptionHandler(subscriber::onError);
                        readStream.exceptionHandler(subscriber::onError).endHandler(v -> {
                            req.end();
                            subscriber.onSuccess(v);
                        });
                        Pump.pump(readStream, req).start();
                    });
                });
    }

    private Single<HttpClient> createHttpClientForFileStorage(Future<Void> closed) {
        return discovery.getRecordObservable(r -> r.getName().equals("file-storage"))
            .map(discovery::getReference)
            .flatMap(reference -> {
                HttpClient client = new HttpClient(reference.get());
                closed.setHandler(ar -> {
                    reference.release();
                });
                return Observable.just(client);
            })
            .toSingle();
    }
}
