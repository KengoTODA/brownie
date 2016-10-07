package jp.skypencil.brownie;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class CommonModuleTest {
    @Test
    public void testIdGeneratorIsSingleton() {
        Injector injector = Guice.createInjector(new CommonModule());
        IdGenerator first = injector.getInstance(IdGenerator.class);
        IdGenerator second = injector.getInstance(IdGenerator.class);
        assertThat(first).isSameAs(second);
    }
}
