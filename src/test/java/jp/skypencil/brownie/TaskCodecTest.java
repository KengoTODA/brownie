package jp.skypencil.brownie;

import io.vertx.core.buffer.Buffer;
import jp.skypencil.brownie.event.VideoUploadedEvent;

import java.util.Collections;
import java.util.UUID;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class TaskCodecTest {

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

}
