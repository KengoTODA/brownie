package jp.skypencil.brownie;

import java.util.UUID;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import org.springframework.util.MimeType;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.Value;

@Value
public class ThumbnailMetadata {
    @Nonnull
    UUID id;

    @Nonnull
    @FileId
    UUID videoId;

    @Nonnull
    MimeType mimeType;

    @Nonnegative
    long contentLength;

    @Nonnegative
    int width;

    @Nonnegative
    int height;

    @Nonnegative
    double second;

    public static ThumbnailMetadata valueOf(JsonObject obj) {
        return new ThumbnailMetadata(
                UUID.fromString(obj.getString("id")),
                UUID.fromString(obj.getString("video_id")),
                MimeType.valueOf(obj.getString("mime_type")),
                obj.getLong("content_length"),
                obj.getInteger("width"),
                obj.getInteger("height"),
                obj.getDouble("second"));
    }

    public JsonArray toJsonArray() {
        return new JsonArray()
                .add(id.toString())
                .add(videoId.toString())
                .add(mimeType.toString())
                .add(contentLength)
                .add(width)
                .add(height)
                .add(second);
    }
}
