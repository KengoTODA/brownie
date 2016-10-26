package jp.skypencil.brownie.encoder;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.asyncsql.AsyncSQLClient;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;

public class EncoderModuleTest {

    @Test
    public void test() {
        Injector injector = Guice.createInjector(new EncoderModule(
                mock(Vertx.class), mock(AsyncSQLClient.class),
                mock(ServiceDiscovery.class)));
        assertThat(injector.getInstance(FileEncoder.class))
                .isInstanceOf(FileEncoderFFmpeg.class);
    }

}
