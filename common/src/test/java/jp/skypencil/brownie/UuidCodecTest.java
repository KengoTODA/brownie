package jp.skypencil.brownie;

import static com.google.common.truth.Truth.assertThat;

import java.util.UUID;

import org.junit.Test;

import io.vertx.core.buffer.Buffer;

public class UuidCodecTest {
    @Test
    public void testEncodeToWire() {
        UuidCodec codec = new UuidCodec();
        UUID uuid = UUID.randomUUID();
        Buffer buffer = Buffer.buffer();

        codec.encodeToWire(buffer, uuid);
        assertThat(buffer.toString()).isEqualTo(uuid.toString());
    }

    @Test
    public void testDncodeFromWire() {
        UuidCodec codec = new UuidCodec();
        UUID uuid = UUID.randomUUID();
        Buffer buffer = Buffer.buffer(uuid.toString());

        UUID decoded = codec.decodeFromWire(0, buffer);
        assertThat(decoded).isEqualTo(uuid);
    }

    @Test
    public void testTransform() {
        UuidCodec codec = new UuidCodec();
        UUID uuid = UUID.randomUUID();

        assertThat(codec.transform(uuid)).isSameAs(uuid);
    }

    @Test
    public void testName() {
        UuidCodec codec = new UuidCodec();
        assertThat(codec.name()).isEqualTo("UUID Codec");
    }

    @Test
    public void testSystemCodecID() {
        UuidCodec codec = new UuidCodec();
        assertThat(codec.systemCodecID()).isEqualTo(-1);
    }
}
