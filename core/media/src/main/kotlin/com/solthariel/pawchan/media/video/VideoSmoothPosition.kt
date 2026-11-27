package com.solthariel.pawchan.media.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import kotlin.math.abs

/**
 * Плавное время для ползунка и тонкой полоски. Плеер сообщает позицию раз в 200 мс, поэтому между
 * опросами время досчитывается по кадрам с учётом скорости; новый опрос сразу поправляет оценку.
 */
@Composable
internal fun rememberSmoothVideoPosition(
    status: VideoPlaybackStatus,
    speed: Float = 1f,
): State<Long> {
    val shown = remember { mutableLongStateOf(status.position) }
    val running = status.playing && status.stage == VideoStage.READY
    LaunchedEffect(status.position, status.duration, running, speed) {
        shown.longValue = status.position
        if (running) followVideoPosition(status, speed) { shown.longValue = it }
    }
    return shown
}

/**
 * Досчитывает время по кадрам только до предела в 400 мс после опроса: дальше оценка всё равно стоит на месте,
 * а лишние кадры не дают экрану (и тестам) успокоиться. Следующий опрос плеера запускает досчёт заново.
 */
internal suspend fun followVideoPosition(
    status: VideoPlaybackStatus,
    speed: Float,
    show: (Long) -> Unit,
) {
    val start = withFrameMillis { it }
    var elapsed = 0L
    while (elapsed < MAX_EXTRAPOLATION_MS) {
        withFrameMillis { now ->
            elapsed = now - start
            show(smoothVideoPosition(status.position, elapsed, speed, status.duration))
        }
    }
}

/** Скорость воспроизведения как множитель (0.5, 1, 1.5, 2). */
@Composable
internal fun rememberVideoSpeed(controls: VideoSoundControls): Float =
    controls.sound
        .collectAsState()
        .value.speed.milli / SPEED_MILLI

/** Досчёт не уходит дальше 400 мс от последнего опроса: при подвисании полоска не убегает вперёд. */
internal fun smoothVideoPosition(
    sample: Long,
    elapsedMillis: Long,
    speed: Float,
    duration: Long,
): Long {
    val ahead = (elapsedMillis.coerceIn(0L, MAX_EXTRAPOLATION_MS) * speed).toLong()
    val end = if (duration > 0L) duration else Long.MAX_VALUE
    return (sample + ahead).coerceAtMost(maxOf(end, sample))
}

/** После отпускания ползунок держит выбранное место, пока плеер не дойдёт до него (не прыгает назад). */
internal fun seekSettled(
    position: Long,
    target: Long,
): Boolean = abs(position - target) <= SEEK_SETTLE_MS

internal const val SEEK_SETTLE_TIMEOUT_MS = 1_500L
private const val SEEK_SETTLE_MS = 1_000L
private const val MAX_EXTRAPOLATION_MS = 400L
private const val SPEED_MILLI = 1000f
