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

    private Observable<Buffer> downloadVideoFromFileStorage(UUID videoId) {
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

    private Single<File> saveVideoToLocal(UUID videoId) {
        UUID localFileId = idGenerator.generateUuidV1();
        File localFile = new File(directory, localFileId.toString());
        return vertx.fileSystem()
            .openObservable(localFile.getAbsolutePath(), new OpenOptions().setCreateNew(true))
            .toSingle()
            .flatMap(asyncFile -> {
                return downloadVideoFromFileStorage(videoId)
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
        return createHttpClientForFileStorage()
            .flatMap(client -> {
                HttpClientRequest req = client.post("/file/")
                    .putHeader("File-Name", "Thumbnail.jpg")
                    .putHeader(HttpHeaders.CONTENT_LENGTH.toString(), Long.toString(tuple._2.getContentLength()))
                    .putHeader(HttpHeaders.CONTENT_TYPE.toString(), tuple._2.getMimeType().toString());
                Observable<HttpClientResponse> result = req.toObservable();
                vertx.fileSystem().openObservable(tuple._1.getAbsolutePath(), new OpenOptions().setCreate(false))
                    .flatMap(AsyncFile::toObservable)
                    .subscribe(req::write, error -> {
                        throw new RuntimeException("Failed to load data from local video file", error);
                    }, req::end);
                return result.toSingle();
            })
            .map(res -> {
                if (res.statusCode() != 200) {
                    throw new RuntimeException("Failed to store thumbnail. Status code is: " + res.statusCode()
                            + ", status message is: " + res.statusMessage());
                }
                return metadata;
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
