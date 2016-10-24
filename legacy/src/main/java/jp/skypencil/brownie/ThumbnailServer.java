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
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.eventbus.Message;
import io.vertx.rxjava.core.eventbus.MessageConsumer;
import io.vertx.rxjava.core.file.AsyncFile;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpClientResponse;
import io.vertx.rxjava.core.streams.Pump;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
import jp.skypencil.brownie.registry.ThumbnailMetadataRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.Single;
import scala.Tuple2;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class ThumbnailServer extends AbstractVerticle {
    private final ThumbnailGenerator thumbnailGenerator;

    private final ThumbnailMetadataRegistry thumbnailMetadataRegistry;

    private final ServiceDiscovery discovery;

    private final String directory = createDirectory();

    private final IdGenerator idGenerator;

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

    private Single<HttpClientResponse> downloadVideoFromFileStorage(UUID videoId, Future<Void> closed) {
        return createHttpClientForFileStorage(closed)
                .flatMap(client -> {
                    return RxHelper.get(client, "/file/" + videoId).toSingle();
                });
    }

    private Single<File> saveVideoToLocal(UUID videoId) {
        UUID localFileId = idGenerator.generateUuidV1();
        File localFile = new File(directory, localFileId.toString());
        Future<Void> closed = Future.future();
        Single<File> result = vertx.fileSystem()
            .openObservable(localFile.getAbsolutePath(), new OpenOptions().setCreateNew(true))
            .toSingle()
            .zipWith(downloadVideoFromFileStorage(videoId, closed), Tuple2::apply)
            .flatMap(tuple -> {
                AsyncFile asyncFile = tuple._1;
                HttpClientResponse readStream = tuple._2;
                return Single.create(subscriber -> {
                    asyncFile.exceptionHandler(subscriber::onError);
                    readStream.exceptionHandler(subscriber::onError).endHandler(v -> {
                        closed.complete(v);
                        asyncFile.close(ar -> {
                            if (ar.failed()) {
                                throw new RuntimeException("Failed to close file", ar.cause());
                            }
                            subscriber.onSuccess(localFile);
                        });
                    });
                    Pump.pump(readStream, asyncFile).start();
                });
            });
        return result.doAfterTerminate(closed::complete);
    }

    private void registerEventListeners() {
        log.info("Thumbnail server has been started");

        // TODO vertx's event bus may lose message. http://vertx.io/docs/vertx-core/java/#_the_theory
        // To achieve the choreography, we need to persist event.
        MessageConsumer<UUID> consumer = vertx.eventBus().consumer("generate-thumbnail");
        consumer.toObservable().map(Message::body).subscribe(videoId -> {
            log.info("Received request to generate thumbnail for videoId {}", videoId);
            saveVideoToLocal(videoId)
                .flatMap(video -> {
                    return thumbnailGenerator.generate(video, 1_000);
                })
                .map(thumbnail -> {
                    return generateMetadata(Tuple2.apply(thumbnail, videoId));
                })
                .flatMap(this::upload)
                .map(thumbnailMetadataRegistry::store)
                .subscribe();
        });
    }

    Tuple2<File, ThumbnailMetadata> generateMetadata(Tuple2<File, UUID> thumbnailAndVideoId) {
        UUID id = UUID.fromString(new com.eaio.uuid.UUID().toString());
        UUID videoId = thumbnailAndVideoId._2();
        MimeType mimeType = MimeType.valueOf("image/jpg");
        File thumbnail = thumbnailAndVideoId._1();
        ThumbnailMetadata metadata = new ThumbnailMetadata(id, videoId, mimeType, thumbnail.length(), 120, 90, 0);

        return Tuple2.apply(thumbnail, metadata);
    }

    Single<ThumbnailMetadata> upload(Tuple2<File, ThumbnailMetadata> tuple) {
        ThumbnailMetadata metadata = tuple._2();
        Future<Void> closed = Future.future();
        return createHttpClientForFileStorage(closed)
            .flatMap(client -> {
                HttpClientRequest req = client.post("/file/")
                    .setChunked(true)
                    .putHeader("File-Name", "Thumbnail.jpg")
                    .putHeader(HttpHeaders.CONTENT_LENGTH.toString(), Long.toString(tuple._2.getContentLength()))
                    .putHeader(HttpHeaders.CONTENT_TYPE.toString(), tuple._2.getMimeType().toString());
                vertx.fileSystem().openObservable(tuple._1.getAbsolutePath(), new OpenOptions().setCreate(false))
                    .subscribe(asyncFile -> {
                        asyncFile.endHandler(v -> {
                            req.end();
                        });
                        Pump.pump(asyncFile, req).start();
                    }, error -> {
                        throw new RuntimeException("Failed to load data from local video file", error);
                    });
                return req.toObservable().toSingle();
            })
            .map(res -> {
                if (res.statusCode() != 200) {
                    throw new RuntimeException("Failed to store thumbnail. Status code is: " + res.statusCode()
                            + ", status message is: " + res.statusMessage());
                }
                return metadata;
            })
            .doAfterTerminate(closed::complete);
    }

    private Single<HttpClient> createHttpClientForFileStorage(Future<Void> closed) {
        Single<HttpClient> clientSingle = discovery.getRecordObservable(r -> r.getName().equals("file-storage"))
            .map(discovery::getReference)
            .flatMap(reference -> {
                HttpClient client = new HttpClient(reference.get());
                closed.setHandler(v -> {
                    reference.release();
                });
                return Observable.just(client);
            })
            .toSingle();
        return clientSingle;
    }
}
