package com.solthariel.pawchan.media.metadata

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.coroutineContext

/** Reads the selected bytes, including restored drafts, without depending on a provider URI. */
fun localMediaMetadataSource(
    context: Context,
    content: ByteArray,
    video: Boolean,
): MediaMetadataSource {
    val appContext = context.applicationContext
    return MediaMetadataSource {
        withContext(Dispatchers.IO) {
            coroutineContext.ensureActive()
            readLocalMetadata(appContext.cacheDir, content, video)
        }
    }
}

private suspend fun readLocalMetadata(
    cacheDirectory: File,
    content: ByteArray,
    video: Boolean,
): MediaMetadata? =
    try {
        val file = File.createTempFile("attachment-metadata-", ".tmp", cacheDirectory)
        try {
            file.outputStream().use { output ->
                var offset = 0
                while (offset < content.size) {
                    coroutineContext.ensureActive()
                    val length = minOf(METADATA_WRITE_CHUNK, content.size - offset)
                    output.write(content, offset, length)
                    offset += length
                }
            }
            coroutineContext.ensureActive()
            if (video) localVideoMetadata(file) else imageMetadata(file)
        } finally {
            file.delete()
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: IOException) {
        null
    } catch (_: RuntimeException) {
        null
    }

private fun localVideoMetadata(file: File): MediaMetadata? {
    val extractor = MediaExtractor()
    try {
        extractor.setDataSource(file.path)
        repeat(extractor.trackCount) { index ->
            val format = extractor.getTrackFormat(index)
            val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith("video/")) {
                val values = localVideoValues(format, mime)
                val duration = format.numberLabel(MediaFormat.KEY_DURATION)?.toLongOrNull() ?: 0L
                return videoMetadata(values, duration / MICROS_PER_MILLISECOND, file.length())
            }
        }
        return null
    } finally {
        extractor.release()
    }
}

private fun localVideoValues(
    format: MediaFormat,
    mime: String,
): Map<String, String> =
    buildMap {
        put("video_format", localVideoCodec(mime))
        format.numberLabel(MediaFormat.KEY_WIDTH)?.let { put("width", it) }
        format.numberLabel(MediaFormat.KEY_HEIGHT)?.let { put("height", it) }
        format.numberLabel(MediaFormat.KEY_FRAME_RATE)?.let { put("frame_rate", it) }
    }

private fun MediaFormat.numberLabel(key: String): String? = if (containsKey(key)) getNumber(key)?.toString() else null

internal fun localVideoCodec(mime: String): String =
    when (mime.lowercase(Locale.ROOT)) {
        "video/avc" -> "H.264"
        "video/hevc" -> "H.265"
        "video/x-vnd.on2.vp8" -> "VP8"
        "video/x-vnd.on2.vp9" -> "VP9"
        "video/av01" -> "AV1"
        "video/mp4v-es" -> "MPEG-4"
        "video/3gpp" -> "H.263"
        else -> mime.substringAfter('/').uppercase(Locale.ROOT)
    }

private const val METADATA_WRITE_CHUNK = 64 * 1024
private const val MICROS_PER_MILLISECOND = 1000L
