package jp.skypencil.brownie;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class FileEncoder {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private Vertx vertx;

    @Resource
    public void setVertx(Vertx vertx) {
        this.vertx = vertx;
    }

    @PostConstruct
    public void listenEvent() {
        vertx.eventBus().localConsumer("file-uploaded", (Message<String> message) -> {
            File uploadedFile = new File(message.body());
            logger.debug("received {}", uploadedFile);

            try (BufferedReader reader = Files.newBufferedReader(uploadedFile.toPath())) {
//                reader.lines().forEach(System.out::println);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
}
