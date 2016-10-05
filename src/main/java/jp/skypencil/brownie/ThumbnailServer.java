package jp.skypencil.brownie;

import java.io.File;
import java.util.UUID;

import javax.inject.Inject;

import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.eventbus.Message;
import io.vertx.rxjava.core.eventbus.MessageConsumer;
import jp.skypencil.brownie.registry.ThumbnailMetadataRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import rx.Single;
import scala.Tuple2;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class ThumbnailServer extends AbstractVerticle {
    private final FileTransporter fileTransporter;

    private final ThumbnailGenerator thumbnailGenerator;

    private final ThumbnailMetadataRegistry thumbnailMetadataRegistry;

    @Override
    public void start() throws Exception {
        registerEventListeners();
    }

    private void registerEventListeners() {
        log.info("Thumbnail server has been started");

        // TODO vertx's event bus may lose message. http://vertx.io/docs/vertx-core/java/#_the_theory
        // To achieve the choreography, we need to persist event.
        MessageConsumer<UUID> consumer = vertx.eventBus().consumer("generate-thumbnail");
        consumer.toObservable().map(Message::body).subscribe(videoId -> {
            log.info("Received request to generate thumbnail for videoId {}", videoId);
            fileTransporter.download(videoId)
                .map(Tuple2::_2)
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
        return fileTransporter.upload(
                metadata.getId(),
                "Thumbnail.jpg",
                tuple._1(), metadata.getMimeType())
               .map(v -> metadata);
    }
}
