package com.solthariel.pawchan.media

import android.content.Context
import android.widget.Scroller
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

internal class ImageViewerTransitions(
    private val scope: CoroutineScope,
    private val context: Context?,
    private val motion: ImageViewerMotion,
) {
    private var animation: Job? = null
    private var scroller: Scroller? = null

    /** Переход на соседний файл, решённый при отпускании пальца и ждущий конца анимации страницы. */
    private var pendingPage: (() -> Unit)? = null

    /**
     * Новое касание во время перелистывания: решённый переход выполняется сразу, а не отменяется.
     * Раньше касание обрывало анимацию до вызова перехода, и страница отскакивала назад
     * (при быстром листании «выбрасывало» на прежнее видео). Возвращает true, если переход выполнен.
     */
    fun interrupt(): Boolean {
        val page = pendingPage
        stop()
        page?.invoke()
        return page != null
    }

    fun stop() {
        pendingPage = null
        animation?.cancel()
        animation = null
        scroller?.forceFinished(true)
        scroller = null
    }

    fun zoom(target: ImageTransform) {
        val start = motion.transform
        animation =
            scope.launch {
                Animatable(0f).animateTo(
                    1f,
                    tween(ReferenceMediaMotion.ZOOM_MS, easing = ReferenceMediaMotion.zoomEasing),
                ) {
                    motion.frame(
                        ImageTransform(
                            start.scale + (target.scale - start.scale) * value,
                            start.x + (target.x - start.x) * value,
                            start.y + (target.y - start.y) * value,
                        ).bounded(motion.viewport),
                        Offset.Zero,
                        false,
                    )
                }
            }
    }

    fun release(
        result: ImageRelease,
        velocity: Offset,
        action: (() -> Unit)?,
    ) {
        stop()
        animation =
            scope.launch {
                when {
                    !motion.closing && motion.displacement == Offset.Zero -> {
                        settleScale()
                        if (result == ImageRelease.FLING) fling(velocity)
                    }

                    result == ImageRelease.CLOSE && action != null -> {
                        close(velocity.y, action)
                    }

                    motion.closing -> {
                        restore()
                    }

                    else -> {
                        page(result, velocity.x, action)
                    }
                }
            }
    }

    private suspend fun close(
        velocity: Float,
        action: () -> Unit,
    ) {
        val extent = (motion.viewport.height + motion.viewport.fittedHeight * motion.transform.scale) / 2f
        val target = if (motion.displacement.y < 0f) -extent - motion.transform.y else extent - motion.transform.y
        val start = motion.displacement.y
        val duration = ReferenceMediaMotion.closeDuration(target - start, velocity)
        motion.fadeWindow(0f)
        Animatable(0f).animateTo(1f, tween(MEDIA_WINDOW_EXIT_MS, easing = LinearEasing)) {
            val travel = (value * MEDIA_WINDOW_EXIT_MS / duration).coerceAtMost(1f)
            motion.frame(shift = Offset(0f, start + (target - start) * travel), close = true)
            motion.fadeWindow(value)
        }
        action()
    }

    private suspend fun restore() {
        Animatable(motion.displacement, Offset.VectorConverter).animateTo(
            Offset.Zero,
            tween(ReferenceMediaMotion.RESTORE_MS, easing = ReferenceMediaMotion.restoreEasing),
        ) {
            motion.frame(shift = value)
        }
        motion.frame(shift = Offset.Zero, close = false)
    }

    private suspend fun page(
        result: ImageRelease,
        velocity: Float,
        action: (() -> Unit)?,
    ) {
        val target =
            when {
                action == null -> 0f
                result == ImageRelease.PREVIOUS -> motion.pageExtent
                else -> -motion.pageExtent
            }
        val duration =
            ReferenceMediaMotion.pageDuration(
                target - motion.displacement.x,
                motion.viewport.width,
                velocity,
                MIN_FLING_DP * (context?.resources?.displayMetrics?.density ?: 1f),
            )
        pendingPage = action
        Animatable(motion.displacement.x).animateTo(
            target,
            tween(duration, easing = ReferenceMediaMotion.pagingEasing),
        ) { motion.frame(shift = Offset(value, 0f)) }
        pendingPage = null
        if (action != null) {
            action()
        } else {
            motion.frame(shift = Offset.Zero, close = false)
        }
    }

    private suspend fun settleScale() {
        if (motion.transform.scale < motion.viewport.minimumScale) {
            val target = ImageTransform(motion.viewport.minimumScale)
            val start = motion.transform
            Animatable(0f).animateTo(
                1f,
                tween(ReferenceMediaMotion.ZOOM_MS, easing = ReferenceMediaMotion.zoomEasing),
            ) {
                motion.frame(
                    ImageTransform(
                        start.scale + (target.scale - start.scale) * value,
                        start.x * (1f - value),
                        start.y * (1f - value),
                    ).bounded(motion.viewport),
                )
            }
        }
    }

    private suspend fun fling(velocity: Offset) {
        val host = context ?: return
        if (abs(velocity.x) + abs(velocity.y) == 0f) return
        val xLimit =
            ((motion.viewport.fittedWidth * motion.transform.scale - motion.viewport.width) / 2f).coerceAtLeast(0f)
        val yLimit =
            ((motion.viewport.fittedHeight * motion.transform.scale - motion.viewport.height) / 2f).coerceAtLeast(0f)
        val fling = Scroller(host)
        scroller = fling
        fling.fling(
            motion.transform.x.roundToInt(),
            motion.transform.y.roundToInt(),
            velocity.x.roundToInt(),
            velocity.y.roundToInt(),
            -xLimit.roundToInt(),
            xLimit.roundToInt(),
            -yLimit.roundToInt(),
            yLimit.roundToInt(),
        )
        while (!fling.isFinished) {
            withFrameNanos { }
            if (fling.computeScrollOffset()) {
                motion.frame(
                    motion.transform
                        .copy(
                            x = fling.currX.toFloat(),
                            y = fling.currY.toFloat(),
                        ).bounded(motion.viewport),
                )
            }
        }
        scroller = null
    }
}

private const val MIN_FLING_DP = 400f

internal const val MEDIA_WINDOW_EXIT_MS = 150
