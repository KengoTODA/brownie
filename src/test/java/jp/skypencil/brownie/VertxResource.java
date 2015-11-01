package jp.skypencil.brownie;

import lombok.Getter;
import io.vertx.core.Vertx;

import org.junit.rules.ExternalResource;

public class VertxResource extends ExternalResource {
    @Getter
    private Vertx vertx;

    @Override
    protected void before() throws Throwable {
        super.before();
        vertx = Vertx.vertx();
    }

    @Override
    protected void after() {
        vertx.close();
        super.after();
    }
}
