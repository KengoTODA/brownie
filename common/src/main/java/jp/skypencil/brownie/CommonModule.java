package jp.skypencil.brownie;

import com.google.inject.AbstractModule;

public class CommonModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(IdGenerator.class).toInstance(new IdGenerator());
    }
}
