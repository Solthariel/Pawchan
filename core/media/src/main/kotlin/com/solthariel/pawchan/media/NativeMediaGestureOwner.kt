package com.solthariel.pawchan.media

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.VelocityTracker
import android.view.ViewConfiguration
import androidx.compose.ui.geometry.Offset

/** One native pointer stream, including quick-scale and active-pointer handover. */
internal class NativeMediaGestureOwner(
    context: Context,
    private val motion: ImageViewerMotion,
    private val navigation: () -> MediaViewerNavigation,
    private val onClose: () -> Unit,
    onLongPress: () -> Unit,
) {
    private val configuration = ViewConfiguration.get(context)
    private val density = context.resources.displayMetrics.density
    private var session = ImageViewerGestureSession(motion.transform, FLING_DISTANCE_DP * density)
    private var tracker: VelocityTracker? = null
    private var pointer = 0
    private var last = Offset.Zero
    private var alive = true
    private var transformed = false
    private val taps: MediaTapListener =
        MediaTapListener(motion, onClose, onLongPress, { scale.isInProgress }, { alive })
    private val scale: ScaleGestureDetector =
        ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    transformed = true
                    if (!taps.doubleTapDown || detector.previousSpan > configuration.scaledTouchSlop * 2f) {
                        val factor =
                            if (taps.doubleTapDown) {
                                detector.scaleFactor.coerceIn(QUICK_MIN, QUICK_MAX)
                            } else {
                                detector.scaleFactor
                            }
                        session.move(
                            ImageGestureSample(0f, 0f, factor, detector.focusX, detector.focusY, 2),
                            motion.viewport,
                            configuration.scaledTouchSlop.toFloat(),
                        )
                        motion.apply(session, navigation())
                    }
                    return true
                }
            },
        ).apply { isQuickScaleEnabled = true }
    private val detector = GestureDetector(context, taps)

    /** Касание, завершившее перелистывание: до следующего ACTION_DOWN события игнорируются. */
    private var passing = false

    fun touch(event: MotionEvent): Boolean {
        if (!alive || motion.dismissing) return alive
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            // Перелистывание, уже решённое отпусканием, доводится до перехода, а не отскакивает назад;
            // остаток этого касания уходящей странице не нужен.
            passing = motion.interrupt()
            if (!passing) begin(event)
        }
        if (!passing) track(event)
        return true
    }

    private fun track(event: MotionEvent) {
        detector.onTouchEvent(event)
        scale.onTouchEvent(event)
        tracker?.addMovement(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> move(event)
            MotionEvent.ACTION_POINTER_DOWN -> multiTouch(event)
            MotionEvent.ACTION_POINTER_UP -> handover(event)
            MotionEvent.ACTION_UP -> finish(cancelled = false)
            MotionEvent.ACTION_CANCEL -> finish(cancelled = true)
        }
    }

    private fun begin(event: MotionEvent) {
        taps.start()
        motion.stop()
        motion.frame(shift = Offset.Zero, close = false)
        session = ImageViewerGestureSession(motion.transform, FLING_DISTANCE_DP * density)
        pointer = event.getPointerId(0)
        last = Offset(event.x, event.y)
        transformed = false
        tracker?.recycle()
        tracker = VelocityTracker.obtain()
    }

    private fun move(event: MotionEvent) {
        val index = event.findPointerIndex(pointer)
        if (index < 0) return
        val point = Offset(event.getX(index), event.getY(index))
        val pan = point - last
        if (!scale.isInProgress &&
            session.move(
                ImageGestureSample(pan.x, pan.y, 1f, point.x, point.y, 1),
                motion.viewport,
                configuration.scaledTouchSlop.toFloat(),
            )
        ) {
            motion.apply(session, navigation())
        }
        last = point
    }

    private fun multiTouch(event: MotionEvent) {
        transformed = true
        session.move(
            ImageGestureSample(0f, 0f, 1f, event.x, event.y, event.pointerCount),
            motion.viewport,
            configuration.scaledTouchSlop.toFloat(),
        )
        motion.apply(session, navigation())
    }

    private fun handover(event: MotionEvent) {
        if (event.getPointerId(event.actionIndex) == pointer) {
            val other = if (event.actionIndex == 0) 1 else 0
            pointer = event.getPointerId(other)
        }
        val index = event.findPointerIndex(pointer)
        if (index >= 0) last = Offset(event.getX(index), event.getY(index))
        transformed = event.pointerCount > 2
        tracker?.clear()
    }

    private fun finish(cancelled: Boolean) {
        tracker?.computeCurrentVelocity(VELOCITY_UNITS, configuration.scaledMaximumFlingVelocity.toFloat())
        val velocity =
            if (transformed || cancelled) {
                Offset.Zero
            } else {
                Offset(tracker?.getXVelocity(pointer) ?: 0f, tracker?.getYVelocity(pointer) ?: 0f)
            }
        val result =
            if (cancelled) {
                ImageRelease.RESTORE
            } else {
                session.release(motion.viewport, velocity.x, velocity.y, FLING_VELOCITY_DP * density)
            }
        val speed = kotlin.math.max(kotlin.math.abs(velocity.x), kotlin.math.abs(velocity.y))
        val inertia =
            if (result == ImageRelease.FLING && speed < configuration.scaledMinimumFlingVelocity) {
                Offset.Zero
            } else {
                velocity
            }
        if (session.mode != ImageGestureMode.WAITING) motion.finish(result, inertia, navigation(), onClose)
        tracker?.recycle()
        tracker = null
    }

    fun dispose() {
        alive = false
        tracker?.recycle()
        tracker = null
        motion.stop()
    }
}

private const val FLING_DISTANCE_DP = 24f
private const val FLING_VELOCITY_DP = 400f
private const val QUICK_MIN = 0.75f
private const val QUICK_MAX = 1.25f

private const val VELOCITY_UNITS = 1000
