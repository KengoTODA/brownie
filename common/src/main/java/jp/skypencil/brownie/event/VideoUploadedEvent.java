package jp.skypencil.brownie.event;

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
public final class VideoUploadedEvent {
    @Nonnull
    private final UUID id;
    @Nonnull
    private final String uploadedFileName;
    @Nonnull
    private final Set<String> resolutions;
    @Nonnull
    private final Instant registered;

    public VideoUploadedEvent(UUID id, String uploadedFileName, Set<String> resolutions) {
        this(id, uploadedFileName, resolutions, Instant.now());
    }

    public VideoUploadedEvent(UUID id, String uploadedFileName, Set<String> resolutions, Instant registered) {
        this.id = Objects.requireNonNull(id);
        this.uploadedFileName = Objects.requireNonNull(uploadedFileName);
        this.resolutions = new HashSet<>(Objects.requireNonNull(resolutions));
        this.registered = registered;
    }

    @Nonnull
    public JsonArray toJsonArray() {
        return new JsonArray()
            .add(getId().toString())
            .add(getUploadedFileName())
            .add(getResolutions().stream().collect(Collectors.joining(",")))
            .add(getRegistered());
    }

    @Nonnull
    public static VideoUploadedEvent from(JsonArray array) {
        UUID id = UUID.fromString(array.remove(0).toString());
        return from(id, array);
    }

    @Nonnull
    public static VideoUploadedEvent from(UUID id, JsonArray array) {
        String name = array.getString(0);
        Set<String> resolutions = new HashSet<>(Arrays.asList(array.getString(1).split(",")));
        Instant generated = Instant.parse(array.getString(2) + "Z");
        return new VideoUploadedEvent(id, name, resolutions, generated);
    }
}
