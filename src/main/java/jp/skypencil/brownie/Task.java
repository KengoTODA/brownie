package jp.skypencil.brownie;

import java.time.Instant;
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
    @Nonnull
    private final Instant registered;

    Task(UUID key, String uploadedFileName, Set<String> resolutions) {
        this(key, uploadedFileName, resolutions, Instant.now());
    }

    Task(UUID key, String uploadedFileName, Set<String> resolutions, Instant registered) {
        this.key = Objects.requireNonNull(key);
        this.uploadedFileName = Objects.requireNonNull(uploadedFileName);
        this.resolutions = new HashSet<>(Objects.requireNonNull(resolutions));
        this.registered = registered;
    }

    String toJson() {
        return String.format("{\"key\":\"%s\",\"fileName\":\"%s\",\"resolutions\":[%s],\"registered\":%d}",
                key,
                uploadedFileName,
                resolutions.stream()
                    .map(resolution -> "\"" + resolution + "\"")
                    .collect(Collectors.joining(",")),
                registered.toEpochMilli());
    }
}
