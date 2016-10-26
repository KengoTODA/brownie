package jp.skypencil.brownie;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;

public class IdGenerator {
    private TimeBasedGenerator uuidGenerator = Generators.timeBasedGenerator(EthernetAddress.fromInterface());

    /**
     * @return An UUIDv1 instance, which suits for ID on distributed system
     */
    @Nonnull
    public UUID generateUuidV1() {
        return uuidGenerator.generate();
    }
}
