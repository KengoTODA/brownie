package jp.skypencil.brownie;


import static com.google.common.truth.Truth.assertThat;

import java.util.UUID;

import org.junit.Test;

public class BrownieFileNotFoundExceptionTest {

    @Test
    public void testToString() {
        UUID fileId = UUID.randomUUID();
        assertThat(new BrownieFileNotFoundException(fileId).toString())
            .isEqualTo("File not found: fileId = " + fileId);
    }

    @Test
    public void testEquals() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        assertThat(new BrownieFileNotFoundException(a))
            .isEqualTo(new BrownieFileNotFoundException(a));
        assertThat(new BrownieFileNotFoundException(a))
            .isNotEqualTo(new BrownieFileNotFoundException(b));
    }

    @Test
    public void testHashcode() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        assertThat(new BrownieFileNotFoundException(a).hashCode())
            .isEqualTo(new BrownieFileNotFoundException(a).hashCode());
        assertThat(new BrownieFileNotFoundException(a).hashCode())
            .isNotEqualTo(new BrownieFileNotFoundException(b).hashCode());
    }
}
