package jp.skypencil.brownie.thumbnail;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.asyncsql.AsyncSQLClient;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;

public class ThumbnailModuleTest {

    @Test
    public void test() {
        Injector injector = Guice.createInjector(new ThumbnailModule(
                mock(Vertx.class), mock(AsyncSQLClient.class),
                mock(ServiceDiscovery.class)));
        assertThat(injector.getInstance(ThumbnailMetadataRegistry.class))
                .isInstanceOf(ThumbnailMetadataRegistryOnPostgres.class);
        assertThat(injector.getInstance(ThumbnailGenerator.class))
                .isInstanceOf(ThumbnailGeneratorFFmpeg.class);
    }

}
