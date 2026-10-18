package com.solthariel.pawchan.media.metadata

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState

data class MediaMetadataRow(
    val label: String,
    val value: String,
)

data class MediaMetadataSection(
    val title: String,
    val rows: List<MediaMetadataRow>,
)

/**
 * Сведения о файле: разделы для полного окна и одна строка для меню
 * («формат · разрешение · подробность»); пустая — строки нет.
 */
data class MediaMetadata(
    val sections: List<MediaMetadataSection>,
    val summary: String,
)

/** Загружает сведения, когда файл готов; null — узнать нечего. */
fun interface MediaMetadataSource {
    suspend fun load(): MediaMetadata?
}

internal data class MediaMetadataState(
    val loading: Boolean,
    val metadata: MediaMetadata? = null,
)

/** Загружает сведения один раз на открытое меню: краткая сводка и полное окно делят результат. */
@Composable
internal fun rememberMediaMetadata(source: MediaMetadataSource?): MediaMetadataState {
    val state by produceState(MediaMetadataState(loading = source != null), source) {
        if (source != null) value = MediaMetadataState(loading = false, metadata = source.load())
    }
    return state
}

internal class MetadataSectionBuilder(
    private val title: String,
) {
    private val rows = mutableListOf<MediaMetadataRow>()

    fun row(
        label: String,
        value: String?,
    ) {
        if (!value.isNullOrBlank()) rows += MediaMetadataRow(label, value.trim())
    }

    fun build(): MediaMetadataSection? = MediaMetadataSection(title, rows.toList()).takeIf { rows.isNotEmpty() }
}

internal fun metadataSection(
    title: String,
    block: MetadataSectionBuilder.() -> Unit,
): MediaMetadataSection? = MetadataSectionBuilder(title).apply(block).build()

/** «48 кГц», «44.1 кГц». */
internal fun sampleRateLabel(hertz: Int): String = "${decimalLabel(hertz / HERTZ_PER_KILOHERTZ)} кГц"

/** «30», «29.97». */
internal fun decimalLabel(value: Double): String =
    if (value == Math.rint(value)) {
        value.toLong().toString()
    } else {
        "%.2f".format(java.util.Locale.ROOT, value).trimEnd('0').trimEnd('.')
    }

/** «850 кбит/с», «1.2 Мбит/с». */
internal fun bitrateLabel(
    bytes: Long,
    durationMillis: Long,
): String? {
    if (bytes <= 0L || durationMillis <= 0L) return null
    val kilobits = bytes * BITS_PER_BYTE / durationMillis.toDouble()
    return if (kilobits >= KILO) {
        "${"%.1f".format(java.util.Locale.ROOT, kilobits / KILO)} Мбит/с"
    } else {
        "${kilobits.toLong()} кбит/с"
    }
}

/** «H.264 / AVC / …» → «H.264»; имя декодера «H.264 Android MediaCodec decoder» → «H.264». */
internal fun shortCodecName(name: String): String =
    name
        .substringBefore(" / ")
        .substringBefore(" (")
        .let(::codecName)

/** Нативный плеер иногда отдаёт имя декодера вместо формата: суффикс «decoder» убирается. */
internal fun codecName(name: String): String = name.replace(DECODER_SUFFIX, "").trim()

private val DECODER_SUFFIX = Regex("""\s+(Android\s+)?(MediaCodec\s+)?decoder$""", RegexOption.IGNORE_CASE)

internal fun durationLabel(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0L) / MILLIS_PER_SECOND
    val hours = seconds / SECONDS_PER_HOUR
    val minutes = seconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
    val rest = (seconds % SECONDS_PER_MINUTE).toString().padStart(2, '0')
    return if (hours > 0) "$hours:${minutes.toString().padStart(2, '0')}:$rest" else "$minutes:$rest"
}

internal const val SUMMARY_SEPARATOR = " · "
private const val HERTZ_PER_KILOHERTZ = 1000.0
private const val BITS_PER_BYTE = 8
private const val KILO = 1000.0
private const val MILLIS_PER_SECOND = 1000L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3600L
