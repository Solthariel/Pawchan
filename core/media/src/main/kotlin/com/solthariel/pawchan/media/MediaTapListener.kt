package com.solthariel.pawchan.media

import android.view.GestureDetector
import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset

/** Android's confirmation/double-tap/long-press recognizer, as in the reference PhotoView. */
internal class MediaTapListener(
    private val motion: ImageViewerMotion,
    private val onClose: () -> Unit,
    private val onLongPress: () -> Unit,
    private val isScaling: () -> Boolean,
    private val isAlive: () -> Boolean,
) : GestureDetector.SimpleOnGestureListener() {
    var doubleTapDown = false
        private set

    fun start() {
        doubleTapDown = false
    }

    override fun onDown(e: MotionEvent): Boolean = true

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        if (!isAlive()) return false
        val width = motion.viewport.fittedWidth * motion.transform.scale
        val height = motion.viewport.fittedHeight * motion.transform.scale
        val x = e.x - motion.viewport.width / 2f - motion.transform.x
        val y = e.y - motion.viewport.height / 2f - motion.transform.y
        if (kotlin.math.abs(x) <= width / 2f && kotlin.math.abs(y) <= height / 2f) {
            motion.toggleControls()
        } else {
            onClose()
        }
        return true
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        doubleTapDown = true
        return true
    }

    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
        if (isAlive() && e.actionMasked == MotionEvent.ACTION_UP && !isScaling()) motion.doubleTap(Offset(e.x, e.y))
        return true
    }

    override fun onLongPress(e: MotionEvent) {
        if (isAlive() && !isScaling()) onLongPress()
    }
}
