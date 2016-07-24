package jp.skypencil.brownie.registry;

public class FileMetadataRegistryOnMemoryTest extends FileMetadataRegistryTest {

    @Override
    FileMetadataRegistry createRegistry() {
        return new FileMetadataRegistryOnMemory();
    }

}
