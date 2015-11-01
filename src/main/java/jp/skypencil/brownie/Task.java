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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result
                + ((resolutions == null) ? 0 : resolutions.hashCode());
        result = prime
                * result
                + ((uploadedFileName == null) ? 0 : uploadedFileName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Task other = (Task) obj;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        if (resolutions == null) {
            if (other.resolutions != null)
                return false;
        } else if (!resolutions.equals(other.resolutions))
            return false;
        if (uploadedFileName == null) {
            if (other.uploadedFileName != null)
                return false;
        } else if (!uploadedFileName.equals(other.uploadedFileName))
            return false;
        return true;
    }
}
