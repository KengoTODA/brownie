package jp.skypencil.brownie;

import java.util.UUID;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class BrownieFileNotFoundException extends RuntimeException {
    private static final long serialVersionUID = -4052854724854084774L;

    @FileId
    @Getter
    private final UUID fileId;

    @Override
    public String toString() {
        return "File not found: fileId = " + fileId;
    }
}
