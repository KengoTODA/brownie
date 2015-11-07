package jp.skypencil.brownie;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.Value;

@Value
@ParametersAreNonnullByDefault
public final class Task {
    @Nonnull
    private final UUID key;
    @Nonnull
    private final String uploadedFileName;
    @Nonnull
    private final Set<String> resolutions;

    Task(String uploadedFileName, Set<String> resolutions) {
        this(generateUuidV1(), uploadedFileName, resolutions);
    }

    Task(UUID key, String uploadedFileName, Set<String> resolutions) {
        this.key = Objects.requireNonNull(key);
        this.uploadedFileName = Objects.requireNonNull(uploadedFileName);
        this.resolutions = new HashSet<>(Objects.requireNonNull(resolutions));
    }

    @Nonnull
    private static UUID generateUuidV1() {
        return UUID.fromString(new com.eaio.uuid.UUID().toString());
    }

    String toJson() {
        return String.format("{\"key\":\"%s\",\"fileName\":\"%s\",\"resolutions\":[%s]}",
                key,
                uploadedFileName,
                resolutions.stream()
                    .map(resolution -> "\"" + resolution + "\"")
                    .collect(Collectors.joining(",")));
    }
}
