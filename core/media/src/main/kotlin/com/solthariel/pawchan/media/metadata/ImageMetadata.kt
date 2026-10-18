package com.solthariel.pawchan.media.metadata

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import coil3.ImageLoader
import com.solthariel.pawchan.core.ui.formatByteSizeForUi
import com.solthariel.pawchan.core.ui.russianPlural
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

/** Сведения о картинке берутся из того же файла в кэше Coil, что показан на экране. */
internal fun imageMetadataSource(
    context: Context,
    imageLoader: ImageLoader,
    url: String,
): MediaMetadataSource =
    MediaMetadataSource {
        withContext(Dispatchers.IO) {
            withTimeoutOrNull(METADATA_WAIT_MILLIS) {
                var result = readImageMetadata(context, imageLoader, url)
                // Пока картинка грузится, файла в кэше ещё нет.
                while (result == null) {
                    delay(METADATA_POLL_MILLIS)
                    result = readImageMetadata(context, imageLoader, url)
                }
                result
            }
        }
    }

private fun readImageMetadata(
    context: Context,
    imageLoader: ImageLoader,
    url: String,
): MediaMetadata? {
    val uri = url.toUri()
    return when (uri.scheme) {
        null, "file" -> {
            uri.path
                ?.let(::File)
                ?.takeIf { it.isFile }
                ?.let(::imageMetadata)
        }

        "content" -> {
            contentImageMetadata(context, uri)
        }

        else -> {
            imageLoader.diskCache?.openSnapshot(url)?.use { imageMetadata(it.data.toFile()) }
        }
    }
}

private fun contentImageMetadata(
    context: Context,
    uri: Uri,
): MediaMetadata? =
    try {
        val copy = File.createTempFile("metadata", null, context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                copy.outputStream().use { input.copyTo(it) }
                imageMetadata(copy)
            }
        } finally {
            copy.delete()
        }
    } catch (_: IOException) {
        null
    }

internal fun imageMetadata(file: File): MediaMetadata? {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, options)
    if (options.outWidth <= 0 || options.outHeight <= 0) return null
    val length = file.length()
    val info = imageHeaderInfo(headerBytes(file, length))
    val format = imageFormatLabel(options.outMimeType)
    val resolution = "${options.outWidth}×${options.outHeight}"
    val frames = info.frames?.takeIf { it > 1 }
    val exif = if (format in EXIF_FORMATS) readExif(file) else ExifSummary()
    val color = listOfNotNull(info.colorType, info.bitDepth?.let { "$it бит" }).joinToString(", ")
    val sections =
        listOfNotNull(
            metadataSection("Изображение") {
                row("Формат", format)
                row("Разрешение", resolution)
                row("Цвет", color)
                row("Цветовое пространство", options.outColorSpace?.name)
                row("Сжатие", info.compression)
                row("Кадры", frames?.toString())
            },
            metadataSection("Файл") {
                row("Размер файла", formatByteSizeForUi(length))
            },
            metadataSection("EXIF") {
                row("Камера", exif.camera)
                row("Дата съёмки", exif.date)
                row("Программа", exif.software)
            },
        )
    return MediaMetadata(sections, imageSummary(format, resolution, length, frames, exif.camera))
}

/** «JPEG · 702×682 · 45 КБ», «GIF · 375×500 · 731 КБ · 8 кадров», «JPEG · 4032×3024 · 3.1 МБ · Pixel 7». */
internal fun imageSummary(
    format: String?,
    resolution: String,
    length: Long,
    frames: Int?,
    camera: String?,
): String {
    val size = length.takeIf { it > 0L }?.let(::formatByteSizeForUi)
    val detail = frames?.let { russianPlural(it, "кадр", "кадра", "кадров") } ?: camera
    return listOfNotNull(format, resolution, size, detail).joinToString(SUMMARY_SEPARATOR)
}

private fun headerBytes(
    file: File,
    length: Long,
): ByteArray =
    if (length <= FULL_SCAN_BYTES) {
        file.readBytes()
    } else {
        RandomAccessFile(file, "r").use { input -> ByteArray(HEADER_BYTES).also(input::readFully) }
    }

private fun imageFormatLabel(mime: String?): String? {
    val subtype = mime?.substringAfter('/')?.lowercase() ?: return null
    return when (subtype) {
        "jpeg", "jpg" -> "JPEG"
        "webp" -> "WebP"
        else -> subtype.uppercase()
    }
}

private data class ExifSummary(
    val camera: String? = null,
    val date: String? = null,
    val software: String? = null,
)

private fun readExif(file: File): ExifSummary =
    try {
        val exif = ExifInterface(file)
        val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim().orEmpty()
        val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim().orEmpty()
        val camera = if (model.startsWith(make, ignoreCase = true)) model else "$make $model"
        val date =
            exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
        ExifSummary(
            camera = camera.trim().takeIf { it.isNotEmpty() },
            date = date?.let(::exifDateLabel),
            software = exif.getAttribute(ExifInterface.TAG_SOFTWARE),
        )
    } catch (_: IOException) {
        ExifSummary()
    }

/** «2021:05:03 14:22:01» → «03.05.2021 14:22:01». */
internal fun exifDateLabel(value: String): String {
    val parts = EXIF_DATE.matchEntire(value.trim())?.groupValues ?: return value
    return "${parts[DAY]}.${parts[MONTH]}.${parts[YEAR]} ${parts[TIME]}"
}

private val EXIF_DATE = Regex("""(\d{4}):(\d{2}):(\d{2}) (\d{2}:\d{2}:\d{2})""")
private const val YEAR = 1
private const val MONTH = 2
private const val DAY = 3
private const val TIME = 4
private val EXIF_FORMATS = setOf("JPEG", "PNG", "WebP", "HEIF", "HEIC")
private const val METADATA_WAIT_MILLIS = 15_000L
private const val METADATA_POLL_MILLIS = 300L
private const val FULL_SCAN_BYTES = 32L * 1024 * 1024
private const val HEADER_BYTES = 256 * 1024
