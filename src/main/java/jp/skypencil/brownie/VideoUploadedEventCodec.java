package jp.skypencil.brownie;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.annotation.ParametersAreNonnullByDefault;

import io.netty.util.CharsetUtil;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import jp.skypencil.brownie.event.VideoUploadedEvent;

@ParametersAreNonnullByDefault
public class VideoUploadedEventCodec implements MessageCodec<VideoUploadedEvent, VideoUploadedEvent> {

    @Override
    public void encodeToWire(Buffer buffer, VideoUploadedEvent s) {
        Set<String> resolutions = s.getResolutions();
        byte[] id = s.getId().toString().getBytes(CharsetUtil.UTF_8);
        byte[] fileName = s.getUploadedFileName().getBytes(CharsetUtil.UTF_8);
        buffer
            .appendInt(id.length)
            .appendBytes(id)
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
    public VideoUploadedEvent decodeFromWire(int pos, Buffer buffer) {
        int idLength = buffer.getInt(pos);
        pos += 4;
        UUID id = UUID.fromString(new String(buffer.getBytes(pos, pos + idLength), CharsetUtil.UTF_8));
        pos += idLength;
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
        return new VideoUploadedEvent(id, fileName, resolutions, Instant.ofEpochMilli(epochMilli));
    }

    @Override
    public VideoUploadedEvent transform(VideoUploadedEvent s) {
        return s;
    }

    @Override
    public String name() {
        return "VideoUploadedEventCodec";
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }

}
