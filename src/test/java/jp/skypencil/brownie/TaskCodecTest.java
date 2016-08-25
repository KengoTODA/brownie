package jp.skypencil.brownie;

import io.vertx.core.buffer.Buffer;

import java.util.Collections;
import java.util.UUID;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class TaskCodecTest {

    @Test
    public void test() {
        UUID taskId = new IdGenerator().generateUuidV1();
        Task task = new Task(taskId, "file", Collections.singleton("resolution"));
        TaskCodec codec = new TaskCodec();
        Buffer buffer = Buffer.buffer();
        codec.encodeToWire(buffer, task);
        Task decodedTask = codec.decodeFromWire(0, buffer);

        assertThat(decodedTask).isEqualTo(task);
    }

}
