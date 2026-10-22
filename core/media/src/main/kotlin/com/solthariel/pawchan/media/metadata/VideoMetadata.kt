package com.solthariel.pawchan.media.metadata

import com.solthariel.pawchan.core.ui.formatByteSizeForUi
import com.solthariel.pawchan.media.video.VideoPlaybackSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Ответ нативного плеера (стабильные ключи контракта метаданных) → разделы окна. */
internal fun videoMetadata(
    values: Map<String, String>,
    durationMillis: Long,
    bytes: Long?,
): MediaMetadata {
    val width = values["width"]?.toIntOrNull()
    val height = values["height"]?.toIntOrNull()
    val resolution = if ((width ?: 0) > 0 && (height ?: 0) > 0) "$width×$height" else null
    val fps = values["frame_rate"]?.toDoubleOrNull()?.takeIf { it > 0.0 }?.let { "${decimalLabel(it)} FPS" }
    val videoCodec = values["video_format"]?.let(::codecName)
    val audioCodec = values["audio_format"]?.let(::codecName)
    val channels = values["channels"]?.toIntOrNull()?.takeIf { it > 0 }
    val sampleRate = values["sample_rate"]?.toIntOrNull()?.takeIf { it > 0 }?.let(::sampleRateLabel)
    val duration = durationMillis.takeIf { it > 0L }?.let(::durationLabel)
    val size = bytes?.takeIf { it > 0L }?.let(::formatByteSizeForUi)
    val sections =
        listOfNotNull(
            metadataSection("Видео") {
                row("Кодек", videoCodec)
                row("Разрешение", resolution)
                row("Частота кадров", fps)
                row("Декодер", values["video_decoder_backend"])
            },
            metadataSection("Аудио") {
                row("Кодек", audioCodec)
                row("Каналы", channels?.let(::channelsLabel))
                row("Частота", sampleRate)
            },
            metadataSection("Файл") {
                row("Длительность", duration)
                row("Размер", size)
                row("Битрейт", bitrateLabel(bytes ?: 0L, durationMillis))
                row("Кодировщик", values["encoder"])
                row("Название", values["title"])
            },
        )
    // «H.264 · 1080×1920 · 4.2 МБ · 29.92 FPS · 0:08»: кодек, разрешение, вес, частота кадров, длительность.
    val summary =
        listOfNotNull(
            videoCodec?.let(::shortCodecName)?.takeIf(String::isNotEmpty),
            resolution,
            size,
            fps,
            duration,
        ).joinToString(SUMMARY_SEPARATOR)
    return MediaMetadata(sections, summary)
}

private fun channelsLabel(count: Int): String =
    when (count) {
        1 -> "1 (моно)"
        2 -> "2 (стерео)"
        else -> count.toString()
    }

/** Ждёт, пока плеер откроет файл: до этого нативная сторона ничего не знает о потоках. */
internal fun videoMetadataSource(
    player: VideoPlaybackSession,
    bytes: Long?,
): MediaMetadataSource =
    MediaMetadataSource {
        val values =
            withTimeoutOrNull(METADATA_WAIT_MILLIS) {
                var values = withContext(Dispatchers.Default) { player.engine.metadata }
                while (values.isEmpty()) {
                    delay(METADATA_POLL_MILLIS)
                    values = withContext(Dispatchers.Default) { player.engine.metadata }
                }
                values
            }
        values?.let { videoMetadata(it, player.status.value.duration, bytes) }
    }

private const val METADATA_WAIT_MILLIS = 15_000L
private const val METADATA_POLL_MILLIS = 300L
