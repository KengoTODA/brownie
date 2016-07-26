package jp.skypencil.brownie.registry;

import org.junit.runner.RunWith;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class FileMetadataRegistryOnPostgresTest extends FileMetadataRegistryTest {
    @Override
    FileMetadataRegistry createRegistry() {
        return new FileMetadataRegistryOnPostgres(vertx, postgresConfig());
    }

    private JsonObject postgresConfig() {
        return new JsonObject()
                .put("host", System.getProperty("db.host", "localhost"))
                .put("port", Integer.valueOf(System.getProperty("db.port", "5432")))
                .put("username", "brownie")
                .put("password", "brownie")
                .put("database", "brownie");
    }
}
