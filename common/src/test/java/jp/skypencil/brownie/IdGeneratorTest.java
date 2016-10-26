package jp.skypencil.brownie;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class IdGeneratorTest {
    @Test
    public void test() {
        IdGenerator generator = new IdGenerator();
        assertThat(generator.generateUuidV1()).isNotEqualTo(generator.generateUuidV1());
    }
}
