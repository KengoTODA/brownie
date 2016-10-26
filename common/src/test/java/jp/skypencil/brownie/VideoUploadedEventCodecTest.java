package jp.skypencil.brownie;

import io.vertx.core.buffer.Buffer;
import jp.skypencil.brownie.event.VideoUploadedEvent;

import java.util.Collections;
import java.util.UUID;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class VideoUploadedEventCodecTest {

    @Test
    public void test() {
        UUID taskId = new IdGenerator().generateUuidV1();
        VideoUploadedEvent task = new VideoUploadedEvent(taskId, "file", Collections.singleton("resolution"));
        VideoUploadedEventCodec codec = new VideoUploadedEventCodec();
        Buffer buffer = Buffer.buffer();
        codec.encodeToWire(buffer, task);
        VideoUploadedEvent decodedTask = codec.decodeFromWire(0, buffer);

        assertThat(decodedTask).isEqualTo(task);
    }

    @Test
    public void testTransform() {
        VideoUploadedEventCodec codec = new VideoUploadedEventCodec();
        UUID taskId = new IdGenerator().generateUuidV1();
        VideoUploadedEvent event = new VideoUploadedEvent(taskId, "file", Collections.singleton("resolution"));

        assertThat(codec.transform(event)).isSameAs(event);
    }

    @Test
    public void testName() {
        VideoUploadedEventCodec codec = new VideoUploadedEventCodec();
        assertThat(codec.name()).isEqualTo("VideoUploadedEvent Codec");
    }

    @Test
    public void testSystemCodecID() {
        VideoUploadedEventCodec codec = new VideoUploadedEventCodec();
        assertThat(codec.systemCodecID()).isEqualTo(-1);
    }
}
