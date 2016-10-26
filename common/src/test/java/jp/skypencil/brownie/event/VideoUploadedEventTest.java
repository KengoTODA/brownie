package jp.skypencil.brownie.event;

import static com.google.common.truth.Truth.assertThat;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import org.junit.Test;

import jp.skypencil.brownie.event.VideoUploadedEvent;

public class VideoUploadedEventTest {
    @Test
    public void testHashCode() {
        Instant now = Instant.now();
        VideoUploadedEvent task = new VideoUploadedEvent(UUID.randomUUID(), "fileName",
                Collections.singleton("resolution"), now);
        assertThat(
                new VideoUploadedEvent(task.getId(), "fileName",
                        Collections.singleton("resolution"), now).hashCode())
                .isEqualTo(task.hashCode());
        assertThat(
                new VideoUploadedEvent(task.getId(), "anotherFileName",
                        Collections.singleton("resolution"), now).hashCode())
                .isNotEqualTo(task.hashCode());
        assertThat(
                new VideoUploadedEvent(task.getId(), "fileName",
                        Collections.singleton("anotherResolution"), now).hashCode())
                .isNotEqualTo(task.hashCode());
        assertThat(
                new VideoUploadedEvent(UUID.randomUUID(), "fileName",
                        Collections.singleton("resolution"), now))
                .isNotEqualTo(task.hashCode());
    }

    @Test
    public void testEquals() {
        Instant now = Instant.now();
        VideoUploadedEvent task = new VideoUploadedEvent(UUID.randomUUID(), "fileName",
                Collections.singleton("resolution"), now);
        assertThat(
                new VideoUploadedEvent(task.getId(), "fileName",
                        Collections.singleton("resolution"), now))
                .isEqualTo(task);
        assertThat(
                new VideoUploadedEvent(task.getId(), "anotherFileName",
                        Collections.singleton("resolution"), now))
                .isNotEqualTo(task);
        assertThat(
                new VideoUploadedEvent(task.getId(), "fileName",
                        Collections.singleton("anotherResolution"), now))
                .isNotEqualTo(task);
        assertThat(
                new VideoUploadedEvent(UUID.randomUUID(), "fileName",
                        Collections.singleton("resolution"), now))
                .isNotEqualTo(task);
    }
}
