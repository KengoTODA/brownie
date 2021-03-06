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
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;

@RunWith(VertxUnitRunner.class)
public class BrownieModuleTest {
    private Vertx vertx;
    private BrownieModule module;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        module = new BrownieModule(vertx, mock(ServiceDiscovery.class));
    }

    @After
    public void cleanUp(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void ensureItCanGenerateVerticles() {
        Injector injector = Guice.createInjector(module);
        injector.getInstance(FrontendServer.class);
    }

}
