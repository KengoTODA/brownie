package jp.skypencil.brownie.registry;

import org.junit.runner.RunWith;

import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class FileMetadataRegistryOnMemoryTest extends FileMetadataRegistryTest {

    @Override
    FileMetadataRegistry createRegistry() {
        return new FileMetadataRegistryOnMemory();
    }

}
