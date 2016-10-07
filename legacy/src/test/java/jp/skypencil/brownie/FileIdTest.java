package jp.skypencil.brownie;

import static org.junit.Assert.*;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.*;

import javax.annotation.meta.When;

import org.junit.Test;

import jp.skypencil.brownie.FileId.Checker;

public class FileIdTest {

    @Test
    public void testCheckObject() {
         Checker checker = new FileId.Checker();
         assertThat(checker.forConstantValue(null, new Object()),
                 is(When.NEVER));
    }

    @Test
    public void testCheckUUID1() {
         Checker checker = new FileId.Checker();
         assertThat(checker.forConstantValue(null, UUID.fromString(new com.eaio.uuid.UUID().toString())),
                 is(When.MAYBE));
    }

    @Test
    public void testCheckUUID4() {
         Checker checker = new FileId.Checker();
         assertThat(checker.forConstantValue(null, UUID.randomUUID()),
                 is(When.NEVER));
    }


}
