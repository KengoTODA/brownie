package jp.skypencil.brownie;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.io.File;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Interface to encode video file.
 */
@ParametersAreNonnullByDefault
public interface FileEncoder {
    /**
     * 
     * @param targetFile
     *          File to convert.
     * @param resolution
     *          Resolution to convert.
     * @param handler
     *          Callback which is called after conversion is finished.
     *          Its result is a {@link File} instance which stores converted data.
     */
    void convert(File targetFile, String resolution, Handler<AsyncResult<File>> handler);
}
