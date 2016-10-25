package jp.skypencil.brownie.thumbnail;

import java.io.File;

import rx.Single;

interface ThumbnailGenerator {
    Single<File> generate(File file, int milliseconds);
}
