package jp.skypencil.brownie.registry;

import jp.skypencil.brownie.FileMetadata;
import io.vertx.core.streams.ReadStream;

public interface FileMetadataReadStream extends ReadStream<FileMetadata>, AutoCloseable {
    @Override
    public void close();
}
