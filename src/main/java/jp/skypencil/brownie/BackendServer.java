package jp.skypencil.brownie;

import java.io.File;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

@Component
@Slf4j
public class BackendServer {
    @Resource
    private Vertx vertx;

    @Resource
    private FileEncoder fileEncoder;

    @Resource
    private FileTransporter fileTransporter;

    @Resource
    private KeyGenerator keyGenerator;

    @PostConstruct
    void registerEventListeners() {
        vertx.eventBus().localConsumer("file-uploaded", (Message<Task> message) -> {
            Task task = message.body();
            fileTransporter.download(task.getKey(), downloaded -> {
                if (downloaded.failed()) {
                    log.error("Failed to download file (key: {}) from distributed file system", task.getKey(), downloaded.cause());
                    message.fail(1, "Failed to download file from distributed file system");
                    return;
                }
                File downloadedFile = downloaded.result();
                log.debug("Downloaded file (key: {}) to {}",
                        task.getKey(),
                        downloadedFile);
                for (String resolution : task.getResolutions()) {
                    fileEncoder.convert(downloadedFile, resolution, converted -> {
                        if (converted.failed()) {
                            log.error("Failed to convert file (key: {}, resolution: {})",
                                    task.getKey(),
                                    resolution,
                                    converted.cause());
                            // TODO do we need to call message.fail() even when file is invalid?
                            // TODO how to handle only a part of resolutions have problem?
                            message.fail(2, "Failed to convert file");
                            return;
                        }
                        File convertedFile = converted.result();
                        String resultFileName = convertedFile.toString();
                        log.info("Converted file (key: {}, resolution: {}) to {}",
                                task.getKey(),
                                resolution,
                                resultFileName);
                        fileTransporter.upload(keyGenerator.generateUuidV1(), task.getUploadedFileName(), convertedFile, MimeType.valueOf("video/mpeg"), uploaded -> {
                            if (uploaded.failed()) {
                                log.error("Failed to upload file (key: {}, resolution: {})",
                                        task.getKey(),
                                        resolution,
                                        uploaded.cause());
                                message.fail(3, "Failed to upload file");
                                return;
                            }
                            log.info("Uploaded file (key: {}, resolution: {})",
                                    task.getKey(),
                                    resolution);
                        });
                    });
                }
            });
        });
    }
}
