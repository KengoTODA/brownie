package jp.skypencil.brownie;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;

import java.util.Collections;
import java.util.UUID;

import org.junit.Test;

public class TaskTest {
    @Test
    public void testHashCode() {
        Task task = new Task(UUID.randomUUID(), "fileName",
                Collections.singleton("resolution"));
        assertThat(
                new Task(task.getKey(), "fileName",
                        Collections.singleton("resolution")).hashCode(),
                is(equalTo(task.hashCode())));
        assertThat(
                new Task(task.getKey(), "anotherFileName",
                        Collections.singleton("resolution")).hashCode(),
                is(not(equalTo(task.hashCode()))));
        assertThat(
                new Task(task.getKey(), "fileName",
                        Collections.singleton("anotherResolution")).hashCode(),
                is(not(equalTo(task.hashCode()))));
        assertThat(
                new Task(UUID.randomUUID(), "fileName",
                        Collections.singleton("resolution")),
                is(not(equalTo(task.hashCode()))));
    }

    @Test
    public void testEquals() {
        Task task = new Task(UUID.randomUUID(), "fileName",
                Collections.singleton("resolution"));
        assertThat(
                new Task(task.getKey(), "fileName",
                        Collections.singleton("resolution")),
                is(equalTo(task)));
        assertThat(
                new Task(task.getKey(), "anotherFileName",
                        Collections.singleton("resolution")),
                is(not(equalTo(task))));
        assertThat(
                new Task(task.getKey(), "fileName",
                        Collections.singleton("anotherResolution")),
                is(not(equalTo(task))));
        assertThat(
                new Task(UUID.randomUUID(), "fileName",
                        Collections.singleton("resolution")),
                is(not(equalTo(task))));
    }

    @Test
    public void testToString() {
        String json = new Task(UUID.randomUUID(), "fileName",
                Collections.singleton("resolution")).toJson();
        assertThat(json, is(containsString("\"key\"")));
        assertThat(json, is(containsString("\"fileName\"")));
        assertThat(json, is(containsString("\"resolutions\"")));
        assertThat(json, is(containsString("[\"resolution\"]")));
    }
}
