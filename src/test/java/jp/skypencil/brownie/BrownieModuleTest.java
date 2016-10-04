package jp.skypencil.brownie;

import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Guice;
import com.google.inject.Injector;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.dns.DnsClient;
import io.vertx.rxjava.ext.asyncsql.AsyncSQLClient;

@RunWith(VertxUnitRunner.class)
public class BrownieModuleTest {
    private Vertx vertx;
    private BrownieModule module;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        module = new BrownieModule(vertx, mock(AsyncSQLClient.class), mock(DnsClient.class), "/tmp");
    }

    @After
    public void cleanUp(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void ensureItCanGenerateVerticles() {
        Injector injector = Guice.createInjector(module);
        injector.getInstance(FrontendServer.class);
        injector.getInstance(BackendServer.class);
        injector.getInstance(ThumbnailServer.class);
    }

}
