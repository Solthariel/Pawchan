package com.solthariel.pawchan.downloads

import android.content.ContentResolver
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.CancellationSignal
import android.os.OperationCanceledException
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.FileDescriptor
import kotlin.coroutines.resume

/** Reads only the saved URI and uses its existing permission. No original URL or HTTP client is accepted here. */
internal suspend fun savedDocumentThumbnail(
    resolver: ContentResolver,
    uri: Uri,
    name: String,
    pixels: Int,
): Bitmap? =
    withContext(SAVED_THUMBNAIL_IO) {
        if (uri.scheme !in LOCAL_SCHEMES) return@withContext null
        suspendCancellableCoroutine { continuation ->
            val signal = CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            val edge = pixels.coerceIn(1, MAX_EDGE)
            try {
                val bitmap =
                    savedThumbnailOrLocal(
                        provider = { resolver.loadThumbnail(uri, Size(edge, edge), signal) },
                        local = {
                            signal.throwIfCanceled()
                            when (savedPreviewKind(name, thumbnailAttempt { resolver.getType(uri) })) {
                                SavedPreviewKind.VIDEO -> savedVideoFrame(resolver, uri, edge, signal)
                                SavedPreviewKind.IMAGE -> savedImage(resolver, uri, edge)
                                SavedPreviewKind.OTHER -> null
                            }
                        },
                    )
                continuation.resume(bitmap)
            } catch (_: OperationCanceledException) {
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }

private fun savedImage(
    resolver: ContentResolver,
    uri: Uri,
    edge: Int,
): Bitmap? =
    ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
        val (width, height) = requireNotNull(savedThumbnailSize(info.size.width, info.size.height, edge))
        decoder.setTargetSize(width, height)
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }

private fun savedVideoFrame(
    resolver: ContentResolver,
    uri: Uri,
    edge: Int,
    signal: CancellationSignal,
): Bitmap? =
    resolver.openAssetFileDescriptor(uri, "r", signal)?.use { asset ->
        MediaMetadataRetriever().use { reader ->
            if (!asset.bind(reader::setDataSource, reader::setDataSource)) return null
            val frame = thumbnailAttempt { reader.smallFrame(-1L, edge) }
            if (frame != null) return frame
            signal.throwIfCanceled()
            val timestamp = firstVideoSample(asset) ?: return null
            reader.smallFrame(timestamp, edge)
        }
    }

private fun MediaMetadataRetriever.smallFrame(
    timeUs: Long,
    edge: Int,
): Bitmap? = getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, edge, edge)

private fun firstVideoSample(asset: AssetFileDescriptor): Long? {
    val extractor = MediaExtractor()
    return try {
        if (!asset.bind(extractor::setDataSource, extractor::setDataSource)) return null
        (0 until extractor.trackCount)
            .firstOrNull { extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true }
            ?.let { track ->
                extractor.selectTrack(track)
                extractor.sampleTime.takeIf { it >= 0 }
            }
    } finally {
        extractor.release()
    }
}

private fun AssetFileDescriptor.bind(
    whole: (FileDescriptor) -> Unit,
    slice: (FileDescriptor, Long, Long) -> Unit,
): Boolean {
    val size = length
    when {
        size > 0L -> slice(fileDescriptor, startOffset, size)
        size == AssetFileDescriptor.UNKNOWN_LENGTH && startOffset == 0L -> whole(fileDescriptor)
        else -> return false
    }
    return true
}

private val LOCAL_SCHEMES = setOf(ContentResolver.SCHEME_CONTENT, ContentResolver.SCHEME_FILE)
private val SAVED_THUMBNAIL_IO = Dispatchers.IO.limitedParallelism(2)
private const val MAX_EDGE = 512
