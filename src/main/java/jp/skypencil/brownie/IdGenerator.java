package jp.skypencil.brownie;

import java.util.UUID;

import javax.annotation.Nonnull;

public class IdGenerator {
    /**
     * @return An UUIDv1 instance, which suits for ID on distributed system
     */
    @Nonnull
    public UUID generateUuidV1() {
        return UUID.fromString(new com.eaio.uuid.UUID().toString());
    }
}
