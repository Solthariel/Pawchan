package com.solthariel.pawchan.composer

import android.content.Context
import coil3.ImageLoader
import coil3.request.ImageRequest

/** Import and cells use the same bounded request, so publishing a file needs no second decode. */
internal fun composerThumbnailRequest(
    context: Context,
    file: ComposerAttachment,
): ImageRequest =
    ImageRequest
        .Builder(context)
        .data(file.preview)
        .memoryCacheKey("composer-thumbnail:${file.id}")
        .size(COMPOSER_THUMBNAIL_SIZE)
        .build()

internal suspend fun prepareComposerThumbnails(
    context: Context,
    files: List<ComposerAttachment>,
    imageLoader: ImageLoader,
) {
    files.filter { it.preview != null && it.options.edit == null }.forEach {
        imageLoader.execute(composerThumbnailRequest(context, it))
    }
}

private const val COMPOSER_THUMBNAIL_SIZE = 384
