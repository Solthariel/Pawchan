package com.solthariel.pawchan.composer

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.coroutines.coroutineContext

internal suspend fun readComposerAttachments(
    context: Context,
    uris: List<Uri>,
    existing: List<ComposerAttachment>,
    rules: PostingRules,
): List<ComposerAttachment> =
    withContext(Dispatchers.IO) {
        require(existing.size + uris.size <= minOf(rules.maxFiles, PostingRules.MAX_LOCAL_FILES)) {
            "Выбранный набор слишком большой для редактора"
        }
        var bytesRemaining =
            PostingRules.MAX_TOTAL_BYTES -
                existing.sumOf {
                    it.source.content.size
                        .toLong()
                }
        val added =
            uris.map { uri ->
                coroutineContext.ensureActive()
                val limit = minOf(rules.maxFileBytes.toLong(), bytesRemaining).toInt()
                require(limit > 0) { "Превышен общий лимит вложений" }
                val attachment = readComposerAttachment(context, uri, limit)
                require(rules.acceptsSelection(listOf(attachment.source))) {
                    "Формат или размер файла не разрешён на этой доске"
                }
                bytesRemaining -= attachment.source.content.size
                attachment
            }
        existing + added
    }

private suspend fun readComposerAttachment(
    context: Context,
    uri: Uri,
    limit: Int,
): ComposerAttachment {
    val resolver = context.contentResolver
    var name = "attachment"
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex >= 0) name = cursor.getString(nameIndex)?.takeIf(String::isNotBlank) ?: name
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                require(cursor.getLong(sizeIndex) <= limit) { "Файл превышает лимит размера" }
            }
        }
    }
    val input = requireNotNull(resolver.openInputStream(uri)) { "Не удалось открыть файл" }
    val bytes = input.use { it.readBounded(limit) }
    val source = ReplyAttachment(name, resolver.getType(uri), bytes)
    val file = ComposerAttachment(source = source, sourceUri = uri.toString())
    return if (file.video) {
        val details = videoDetails(context, uri)
        file.copy(preview = details.preview, dimensions = details.dimensions)
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        file.copy(dimensions = imageDimensions(bounds.outWidth, bounds.outHeight))
    }
}

private suspend fun InputStream.readBounded(limit: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    var read = read(buffer)
    while (read >= 0) {
        coroutineContext.ensureActive()
        total += read
        require(total <= limit) { "Файл превышает лимит размера" }
        output.write(buffer, 0, read)
        read = read(buffer)
    }
    return output.toByteArray()
}

private fun videoDetails(
    context: Context,
    uri: Uri,
): ComposerVideoDetails =
    try {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(context, uri)
            val preview =
                retriever
                    .getScaledFrameAtTime(
                        0,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        VIDEO_THUMBNAIL_SIZE,
                        VIDEO_THUMBNAIL_SIZE,
                    )?.let(::encodeVideoThumbnail)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
            ComposerVideoDetails(preview, imageDimensions(width ?: 0, height ?: 0))
        }
    } catch (_: RuntimeException) {
        ComposerVideoDetails(null, null)
    }

private fun encodeVideoThumbnail(bitmap: android.graphics.Bitmap): ByteArray =
    try {
        ByteArrayOutputStream().use { output ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, VIDEO_THUMBNAIL_QUALITY, output)
            output.toByteArray()
        }
    } finally {
        bitmap.recycle()
    }

private const val VIDEO_THUMBNAIL_SIZE = 384
private const val VIDEO_THUMBNAIL_QUALITY = 80

private data class ComposerVideoDetails(
    val preview: ByteArray?,
    val dimensions: String?,
)

private fun imageDimensions(
    width: Int,
    height: Int,
): String? = if (width > 0 && height > 0) "$width×$height" else null
