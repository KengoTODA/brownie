package jp.skypencil.brownie.registry;

import org.junit.runner.RunWith;

import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class FileMetadataRegistryOnPostgresTest extends FileMetadataRegistryTest {
    @Override
    FileMetadataRegistry createRegistry() {
        return new FileMetadataRegistryOnPostgres("192.168.99.100", vertx);
    }
}
