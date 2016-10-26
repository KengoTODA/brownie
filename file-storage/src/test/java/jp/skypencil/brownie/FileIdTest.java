package jp.skypencil.brownie;

import static com.google.common.truth.Truth.assertThat;

import java.util.UUID;

import javax.annotation.meta.When;

import org.junit.Test;

import jp.skypencil.brownie.FileId.Checker;

public class FileIdTest {

    @Test
    public void testCheckObject() {
         Checker checker = new FileId.Checker();
         assertThat(checker.forConstantValue(null, new Object()))
                 .isSameAs(When.NEVER);
    }

    @Test
    public void testCheckUUID1() {
         Checker checker = new FileId.Checker();
         assertThat(checker.forConstantValue(null, new IdGenerator().generateUuidV1()))
                 .isSameAs(When.MAYBE);
    }

    @Test
    public void testCheckUUID4() {
         Checker checker = new FileId.Checker();
         assertThat(checker.forConstantValue(null, UUID.randomUUID()))
                 .isSameAs(When.NEVER);
    }

}
