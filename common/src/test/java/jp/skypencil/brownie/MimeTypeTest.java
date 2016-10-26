package jp.skypencil.brownie;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class MimeTypeTest {
    @Test
    public void test() {
        assertThat(MimeType.valueOf("text/plain")).isEqualTo(new MimeType("text", "plain"));
    }
}
