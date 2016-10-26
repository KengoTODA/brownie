package jp.skypencil.brownie;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.asyncsql.AsyncSQLClient;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
import jp.skypencil.brownie.fs.MountedFileSystem;
import jp.skypencil.brownie.fs.SharedFileSystem;

public class FileStorageModuleTest {

    @Test
    public void testItDependsOnCommonModule() {
        Injector injector = Guice.createInjector(new FileStorageModule(
                mock(Vertx.class), mock(AsyncSQLClient.class), mock(ServiceDiscovery.class), "mountedDir"));
        assertThat(injector.getInstance(IdGenerator.class))
            .isNotNull();
    }

    @Test
    public void testSharedFileSystem() {
        Injector injector = Guice.createInjector(new FileStorageModule(
                mock(Vertx.class), mock(AsyncSQLClient.class), mock(ServiceDiscovery.class), "mountedDir"));
        assertThat(injector.getInstance(SharedFileSystem.class))
            .isInstanceOf(MountedFileSystem.class);
    }

}
