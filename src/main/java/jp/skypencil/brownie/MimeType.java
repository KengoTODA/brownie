package jp.skypencil.brownie;

import javax.annotation.Nonnull;

import lombok.Value;

@Value
public class MimeType {
    private String primary;
    private String sub;

    @Nonnull
    public static MimeType valueOf(@Nonnull String raw) {
        String[] split = raw.split("/");
        if (split.length != 2) {
            throw new IllegalArgumentException("Found illegal mime_type: " + raw);
        }

        return new MimeType(split[0], split[1]);
    }

    @Override
    public String toString() {
        return primary + "/" + sub;
    }
}
