package jp.skypencil.brownie;

import java.io.File;
import java.util.UUID;

import javax.inject.Inject;

import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.eventbus.Message;
import io.vertx.rxjava.core.eventbus.MessageConsumer;
import jp.skypencil.brownie.event.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.Single;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class EncodeServer extends AbstractVerticle {
    private final FileEncoder fileEncoder;

    private final FileTransporter fileTransporter;

    private final IdGenerator idGenerator;

    @Override
    public void start() throws Exception {
        registerEventListeners();
    }

    private void registerEventListeners() {
        MessageConsumer<VideoUploadedEvent> consumer = vertx.eventBus().localConsumer("file-uploaded");
        consumer.toObservable().subscribe(message -> {
            VideoUploadedEvent task = message.body();
            fileTransporter.download(task.getId()).doOnSuccess(downloadedFile -> {
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

    private Single<FileMetadata> upload(File source, String fileName,
            Message<VideoUploadedEvent> message) {
        UUID id = idGenerator.generateUuidV1();
        return fileTransporter.upload(id, fileName,
                source, MimeType.valueOf("video/mpeg")).doOnError(error -> {
                    log.error("Failed to upload file ({})",
                            source.getAbsolutePath(), error);
                    message.fail(3, "Failed to upload file");
                }).doOnSuccess(v -> {
                    log.info("Uploaded file ({})", source.getAbsolutePath());
                    vertx.eventBus().send("generate-thumbnail", id);
                });
    }
}
