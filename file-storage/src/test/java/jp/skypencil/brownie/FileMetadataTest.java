package jp.skypencil.brownie;

import static com.google.common.truth.Truth.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.Test;

import io.vertx.core.json.JsonObject;

public class FileMetadataTest {
    @Test
    public void testToString() {
        String string = new FileMetadata(UUID.randomUUID(), "name",
                MimeType.valueOf("text/plain"), 0, Instant.now()).toString();
        assertThat(string).contains("name=name");
        assertThat(string).contains("mimeType=text/plain");
    }

    @Test
    public void testHashCode() {
        FileMetadata metadata = new FileMetadata(UUID.randomUUID(), "name",
                MimeType.valueOf("text/plain"), 0, Instant.now());
        int hashCode = metadata.hashCode();
        assertThat(
                new FileMetadata(metadata.getFileId(), "name",
                        MimeType.valueOf("text/plain"), 0,
                        metadata.getGenerated()).hashCode())
                .isEqualTo(hashCode);
        assertThat(new FileMetadata(UUID.randomUUID(), "name",
                MimeType.valueOf("text/plain"), 0, Instant.now()).hashCode())
                .isNotEqualTo(hashCode);
    }

    @Test
    public void testToJsonObject() {
        UUID uuid = UUID.randomUUID();
        Instant instant = Instant.now();
        JsonObject json = new FileMetadata(uuid, "name",
                MimeType.valueOf("text/plain"), 0, instant).toJsonObject();
        assertThat(json.getString("id")).isEqualTo(uuid.toString());
        assertThat(json.getString("name")).isEqualTo("name");
        assertThat(json.getString("mime_type")).isEqualTo("text/plain");
        assertThat(json.getInteger("content_length")).isEqualTo(0);
        assertThat(json.getInstant("generated")).isEqualTo(instant);
    }
}
