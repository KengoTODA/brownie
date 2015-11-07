package jp.skypencil.brownie;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.annotation.ParametersAreNonnullByDefault;

import io.netty.util.CharsetUtil;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

@ParametersAreNonnullByDefault
public class TaskCodec implements MessageCodec<Task, Task> {

    @Override
    public void encodeToWire(Buffer buffer, Task s) {
        Set<String> resolutions = s.getResolutions();
        byte[] key = s.getKey().toString().getBytes(CharsetUtil.UTF_8);
        byte[] fileName = s.getUploadedFileName().getBytes(CharsetUtil.UTF_8);
        buffer
            .appendInt(key.length)
            .appendBytes(key)
            .appendInt(fileName.length)
            .appendBytes(fileName)
            .appendInt(resolutions.size());
        resolutions.stream()
            .map(resolution -> resolution.getBytes(CharsetUtil.UTF_8))
            .forEach(bytes -> {
                buffer.appendInt(bytes.length).appendBytes(bytes);
            });
        buffer.appendLong(s.getRegistered().toEpochMilli());
    }

    @Override
    public Task decodeFromWire(int pos, Buffer buffer) {
        int keyLength = buffer.getInt(pos);
        pos += 4;
        UUID key = UUID.fromString(new String(buffer.getBytes(pos, pos + keyLength), CharsetUtil.UTF_8));
        pos += keyLength;
        int fileNameLength = buffer.getInt(pos);
        pos += 4;
        String fileName = new String(buffer.getBytes(pos, pos + fileNameLength), CharsetUtil.UTF_8);
        pos += fileNameLength;
        int resolutionsCount = buffer.getInt(pos);
        pos += 4;
        Set<String> resolutions = new HashSet<>();
        for (int i = 0; i < resolutionsCount; ++i) {
            int resolutionLength = buffer.getInt(pos);
            pos += 4;
            resolutions.add(new String(buffer.getBytes(pos, pos + resolutionLength), CharsetUtil.UTF_8));
            pos += resolutionLength;
        }
        long epochMilli = buffer.getLong(pos);
        pos += 8;
        return new Task(key, fileName, resolutions, Instant.ofEpochMilli(epochMilli));
    }

    @Override
    public Task transform(Task s) {
        return s;
    }

    @Override
    public String name() {
        return "TaskCodec";
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }

}
