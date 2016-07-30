package jp.skypencil.brownie;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import io.vertx.core.json.JsonArray;
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

    public Task(UUID key, String uploadedFileName, Set<String> resolutions, Instant registered) {
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

    @Nonnull
    public JsonArray toJsonArray() {
        return new JsonArray()
            .add(getKey().toString())
            .add(getUploadedFileName())
            .add(getResolutions().stream().collect(Collectors.joining(",")))
            .add(getRegistered());
    }

    @Nonnull
    public static Task from(JsonArray array) {
        UUID key = UUID.fromString(array.remove(0).toString());
        return from(key, array);
    }

    @Nonnull
    public static Task from(UUID key, JsonArray array) {
        String name = array.getString(0);
        Set<String> resolutions = new HashSet<>(Arrays.asList(array.getString(1).split(",")));
        Instant generated = Instant.parse(array.getString(2) + "Z");
        return new Task(key, name, resolutions, generated);
    }
}
