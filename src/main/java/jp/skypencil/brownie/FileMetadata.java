package jp.skypencil.brownie;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import lombok.Value;

import org.springframework.util.MimeType;

@ThreadSafe
@Value
public class FileMetadata implements Serializable {
    private static final long serialVersionUID = 5668165339485643650L;

    @Nonnull
    @FileId
    UUID fileId;
    @Nonnull
    String name;
    @Nonnull
    MimeType mimeType;
    @Nonnegative
    long contentLength;
    @Nonnull
    Instant generated;

    public String toJson() {
        return String.format("{\"fileId\":\"%s\",\"fileName\":\"%s\",\"mimeType\":\"%s\",\"contentLength\":%d,\"generated\":%d}",
                fileId, name, mimeType, contentLength, generated.toEpochMilli());
    }
}
