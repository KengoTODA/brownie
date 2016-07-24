package jp.skypencil.brownie.registry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

import jp.skypencil.brownie.VertxResource;

public class FileMetadataRegistryOnPostgresTest extends FileMetadataRegistryTest {
    @Rule
    public VertxResource resource = new VertxResource();

    private FileMetadataRegistryOnPostgres registry;

    @Before
    public void setUp() {
        registry = new FileMetadataRegistryOnPostgres("192.168.99.100", resource.getVertx());
    }

    @After
    public void cleanUp() {
        registry.close();
    }

    @Override
    FileMetadataRegistry createRegistry() {
        return registry;
    }

}
