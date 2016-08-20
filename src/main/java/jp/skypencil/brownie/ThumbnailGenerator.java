package jp.skypencil.brownie;

import java.io.File;

import rx.Single;

interface ThumbnailGenerator {
    Single<File> generate(File file, int milliseconds);
}
