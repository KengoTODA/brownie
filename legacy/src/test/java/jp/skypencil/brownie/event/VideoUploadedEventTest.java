package jp.skypencil.brownie.event;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;

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
                        Collections.singleton("resolution"), now).hashCode(),
                is(equalTo(task.hashCode())));
        assertThat(
                new VideoUploadedEvent(task.getId(), "anotherFileName",
                        Collections.singleton("resolution"), now).hashCode(),
                is(not(equalTo(task.hashCode()))));
        assertThat(
                new VideoUploadedEvent(task.getId(), "fileName",
                        Collections.singleton("anotherResolution"), now).hashCode(),
                is(not(equalTo(task.hashCode()))));
        assertThat(
                new VideoUploadedEvent(UUID.randomUUID(), "fileName",
                        Collections.singleton("resolution"), now),
                is(not(equalTo(task.hashCode()))));
    }

    @Test
    public void testEquals() {
        Instant now = Instant.now();
        VideoUploadedEvent task = new VideoUploadedEvent(UUID.randomUUID(), "fileName",
                Collections.singleton("resolution"), now);
        assertThat(
                new VideoUploadedEvent(task.getId(), "fileName",
                        Collections.singleton("resolution"), now),
                is(equalTo(task)));
        assertThat(
                new VideoUploadedEvent(task.getId(), "anotherFileName",
                        Collections.singleton("resolution"), now),
                is(not(equalTo(task))));
        assertThat(
                new VideoUploadedEvent(task.getId(), "fileName",
                        Collections.singleton("anotherResolution"), now),
                is(not(equalTo(task))));
        assertThat(
                new VideoUploadedEvent(UUID.randomUUID(), "fileName",
                        Collections.singleton("resolution"), now),
                is(not(equalTo(task))));
    }

    @Test
    public void testToString() {
        String json = new VideoUploadedEvent(UUID.randomUUID(), "fileName",
                Collections.singleton("resolution")).toJson();
        assertThat(json, is(containsString("\"id\"")));
        assertThat(json, is(containsString("\"fileName\"")));
        assertThat(json, is(containsString("\"resolutions\"")));
        assertThat(json, is(containsString("[\"resolution\"]")));
    }
}