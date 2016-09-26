package jp.skypencil.brownie;

import java.io.File;

import javax.annotation.ParametersAreNonnullByDefault;

import rx.Single;

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
    Single<File> convert(File targetFile, String resolution);
}
