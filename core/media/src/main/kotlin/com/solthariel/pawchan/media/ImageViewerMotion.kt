package com.solthariel.pawchan.media

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope

/** State of the current photo; platform scrollers and animations are owned by its composition. */
internal class ImageViewerMotion(
    scope: CoroutineScope,
    context: Context? = null,
    private val chrome: MediaChromeState = MediaChromeState(),
) {
    var transform by mutableStateOf(ImageTransform())
        private set
    var displacement by mutableStateOf(Offset.Zero)
        private set
    var closing by mutableStateOf(false)
        private set
    var viewport by mutableStateOf(ImageViewport(1f, 1f, 1f, 1f))
        private set
    var windowAlpha by mutableFloatStateOf(1f)
        private set
    var dismissing by mutableStateOf(false)
        private set
    val controlsVisible: Boolean get() = chrome.visible
    val pageGap: Float = ReferenceMediaMotion.PAGE_GAP_DP * (context?.resources?.displayMetrics?.density ?: 1f)
    val pageExtent: Float get() = viewport.width + pageGap
    val imageAlpha: Float get() = if (closing) ReferenceMediaMotion.imageAlpha(displacement.y, viewport.height) else 1f
    val imageScale: Float
        get() = transform.scale * if (closing) ReferenceMediaMotion.imageScale(displacement.y, viewport.height) else 1f
    val backgroundAlpha: Float
        get() = if (closing) ReferenceMediaMotion.backgroundAlpha(displacement.y, viewport.height) else 1f
    private val transitions = ImageViewerTransitions(scope, context, this)

    fun resize(value: ImageViewport) {
        if (value == viewport || dismissing) return
        val moving = displacement != Offset.Zero && viewport.width == value.width && viewport.height == value.height
        if (!moving) transitions.stop()
        val initial =
            (viewport.width == 1f && viewport.height == 1f) ||
                viewport.imageWidth != value.imageWidth || viewport.imageHeight != value.imageHeight
        viewport = value
        transform =
            (
                if (initial) {
                    ImageTransform(value.initialScale)
                } else {
                    transform.copy(scale = transform.scale.coerceIn(value.minimumScale, value.maximumScale))
                }
            ).bounded(value)
        if (!moving) {
            displacement = Offset.Zero
            closing = false
            windowAlpha = 1f
            dismissing = false
        }
    }

    fun stop() {
        transitions.stop()
    }

    /** Касание во время анимации: начатое перелистывание завершается переходом; true — страница уходит. */
    fun interrupt(): Boolean = transitions.interrupt()

    fun reset() {
        if (dismissing) return
        stop()
        windowAlpha = 1f
        dismissing = false
        frame(ImageTransform(viewport.initialScale), Offset.Zero, false)
    }

    fun toggleControls() {
        if (dismissing) return
        chrome.visible = !chrome.visible
    }

    fun doubleTap(position: Offset) {
        if (dismissing) return
        stop()
        transitions.zoom(transform.doubleTap(position.x, position.y, viewport))
    }

    fun apply(
        session: ImageViewerGestureSession,
        navigation: MediaViewerNavigation,
    ) {
        val shift =
            when (session.mode) {
                ImageGestureMode.PAGE -> {
                    val allowed = if (session.travelX > 0f) navigation.previous != null else navigation.next != null
                    Offset(if (allowed) session.travelX.coerceIn(-pageExtent, pageExtent) else 0f, 0f)
                }

                ImageGestureMode.CLOSE -> {
                    Offset(0f, session.travelY * IMAGE_CLOSE_RESISTANCE)
                }

                else -> {
                    Offset.Zero
                }
            }
        frame(session.transform, shift, session.mode == ImageGestureMode.CLOSE)
    }

    fun finish(
        result: ImageRelease,
        velocity: Offset,
        navigation: MediaViewerNavigation,
        onClose: () -> Unit,
    ) {
        val action =
            when (result) {
                ImageRelease.PREVIOUS -> navigation.previous
                ImageRelease.NEXT -> navigation.next
                ImageRelease.CLOSE -> onClose
                else -> null
            }
        transitions.release(result, velocity, action)
    }

    fun fadeWindow(progress: Float) {
        dismissing = true
        val remaining = 1f - progress.coerceIn(0f, 1f)
        windowAlpha = remaining * remaining
    }

    fun frame(
        value: ImageTransform = transform,
        shift: Offset = displacement,
        close: Boolean = closing,
    ) {
        transform = value
        displacement = shift
        closing = close
    }
}
