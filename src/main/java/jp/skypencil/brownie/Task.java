package jp.skypencil.brownie;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class Task {
    private final UUID key;
    private final String uploadedFileName;
    private final Set<String> resolutions;

    Task(String uploadedFileName, Set<String> resolutions) {
        // TODO use UUIDv1
        this(UUID.randomUUID(), uploadedFileName, resolutions);
    }

    Task(UUID key, String uploadedFileName, Set<String> resolutions) {
        this.key = key;
        this.uploadedFileName = uploadedFileName;
        this.resolutions = new HashSet<>(resolutions);
    }

    public UUID getKey() {
        return key;
    }

    public String getUploadedFileName() {
        return uploadedFileName;
    }

    public Set<String> getResolutions() {
        return resolutions;
    }
}
