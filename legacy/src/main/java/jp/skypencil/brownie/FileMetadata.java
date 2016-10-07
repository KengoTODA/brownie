package jp.skypencil.brownie;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import io.vertx.core.json.JsonObject;
import lombok.Value;

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

    public JsonObject toJsonObject() {
        return new JsonObject()
                .put("id", fileId.toString())
                .put("name", name)
                .put("mime_type", mimeType.toString())
                .put("content_length", contentLength)
                .put("generated", generated);
    }
}
