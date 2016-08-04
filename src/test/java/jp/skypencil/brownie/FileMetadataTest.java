package jp.skypencil.brownie;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;

import java.time.Instant;
import java.util.UUID;

import org.junit.Test;
import org.springframework.util.MimeType;

public class FileMetadataTest {
	@Test
	public void testToString() {
		String string = new FileMetadata(UUID.randomUUID(), "name", MimeType.valueOf("text/plain"), 0, Instant.now()).toString();
		assertThat(string, is(containsString("name=name")));
		assertThat(string, is(containsString("mimeType=text/plain")));
	}

	@Test
	public void testHashCode() {
		FileMetadata metadata = new FileMetadata(UUID.randomUUID(), "name", MimeType.valueOf("text/plain"), 0, Instant.now());
		int hashCode = metadata.hashCode();
		assertThat(new FileMetadata(metadata.getFileId(), "name", MimeType.valueOf("text/plain"), 0, metadata.getGenerated()).hashCode(),
				is(equalTo(hashCode)));
		assertThat(new FileMetadata(UUID.randomUUID(), "name", MimeType.valueOf("text/plain"), 0, Instant.now()).hashCode(),
				is(not(equalTo(hashCode))));
	}

	@Test
	public void testToJson() {
		String json = new FileMetadata(UUID.randomUUID(), "name", MimeType.valueOf("text/plain"), 0, Instant.now()).toJson();
		assertThat(json, is(containsString("\"fileId\"")));
		assertThat(json, is(containsString("\"fileName\":\"name\"")));
		assertThat(json, is(containsString("\"text/plain\"")));
	}
}
