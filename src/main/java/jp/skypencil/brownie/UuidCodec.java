package jp.skypencil.brownie;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

public class UuidCodec implements MessageCodec<UUID, UUID> {

    @Override
    public void encodeToWire(Buffer buffer, UUID s) {
        buffer.appendString(s.toString());
    }

    @Override
    public UUID decodeFromWire(int pos, Buffer buffer) {
        byte[] bytes = buffer.getBytes(pos, buffer.length());
        return UUID.fromString(new String(bytes, StandardCharsets.UTF_8));
    }

    @Override
    public UUID transform(UUID s) {
        return s;
    }

    @Override
    public String name() {
        return "UUID Codec";
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }

}
