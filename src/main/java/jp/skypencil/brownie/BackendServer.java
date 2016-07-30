package jp.skypencil.brownie;

import java.io.File;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.Message;
import io.vertx.rxjava.core.eventbus.MessageConsumer;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import rx.Observable;

import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

@Component
@Slf4j
public class BackendServer {
    @Resource
    private Vertx rxJavaVertx;

    @Resource
    private ObservableFileEncoder fileEncoder;

    @Resource
    private ObservableFileTransporter fileTransporter;

    @Resource
    private KeyGenerator keyGenerator;

    @PostConstruct
    void registerEventListeners() {
        MessageConsumer<Task> consumer = rxJavaVertx.eventBus().localConsumer("file-uploaded");
        consumer.toObservable().subscribe(message -> {
            Task task = message.body();
            fileTransporter.download(task.getKey()).doOnNext(downloadedFile -> {
                log.debug("Downloaded file (key: {}) to {}",
                        task.getKey(),
                        downloadedFile);
            }).doOnError(error -> {
                log.error("Failed to download file (key: {}) from distributed file system", task.getKey(), error);
                message.fail(1, "Failed to download file from distributed file system");
            }).flatMap(downloadedFile -> {
                return Observable.from(task.getResolutions()).flatMap(resolution -> {
                    return convert(downloadedFile, resolution, message);
                });
            }).flatMap(convertedFile -> {
                return upload(convertedFile, task.getUploadedFileName(), message);
            }).subscribe();
        });
    }

    private Observable<File> convert(File source, String resolution, Message<Task> message) {
        return fileEncoder.convert(source, resolution)
                .doOnNext(converted -> {
                    log.info("Converted file (path: {}, resolution: {}) to {}",
                            source.getAbsolutePath(),
                            resolution,
                            converted.getAbsoluteFile());
                }).doOnError(error -> {
                    log.error("Failed to convert file (path: {}, resolution: {})",
                            source.getAbsolutePath(),
                            resolution,
                            error);
                    // TODO do we need to call message.fail() even when file is invalid?
                    // TODO how to handle only a part of resolutions have problem?
                    message.fail(2, "Failed to convert file");
                });
    }

    private Observable<Void> upload(File source, String fileName,
            Message<Task> message) {
        return fileTransporter.upload(keyGenerator.generateUuidV1(), fileName,
                source, MimeType.valueOf("video/mpeg")).doOnError(error -> {
                    log.error("Failed to upload file ({})",
                            source.getAbsolutePath(), error);
                    message.fail(3, "Failed to upload file");
                }).doOnNext(v -> {
                    log.info("Uploaded file ({})", source.getAbsolutePath());
                });
    }
}
