package com.solthariel.pawchan

import com.solthariel.pawchan.media.ImageGestureMode
import com.solthariel.pawchan.media.ImageGestureSample
import com.solthariel.pawchan.media.ImageRelease
import com.solthariel.pawchan.media.ImageTransform
import com.solthariel.pawchan.media.ImageViewerGestureSession
import com.solthariel.pawchan.media.ImageViewport
import org.junit.Assert.assertEquals
import org.junit.Test

class ViewerReferenceGestureTest {
    private val viewport = ImageViewport(300f, 400f, 300f, 400f)

    @Test
    fun shortFastSwipeUsesReferenceVelocityNotOldEightHundredThreshold() {
        val session = session(-30f, 0f)
        assertEquals(ImageRelease.NEXT, session.release(viewport, -450f, 0f, 400f))
        assertEquals(ImageRelease.RESTORE, session.release(viewport, -399f, 0f, 400f))
    }

    @Test
    fun slowPagingRequiresHalfViewportAndOppositeFlingCancels() {
        assertEquals(ImageRelease.RESTORE, session(-100f, 0f).release(viewport, 0f, 0f, 400f))
        assertEquals(ImageRelease.NEXT, session(-160f, 0f).release(viewport, 0f, 0f, 400f))
        assertEquals(ImageRelease.RESTORE, session(-160f, 0f).release(viewport, 500f, 0f, 400f))
    }

    @Test
    fun flingDistanceIsDensityScaled() {
        val session = ImageViewerGestureSession(ImageTransform(), flingDistance = 72f)
        session.move(sample(-50f, 0f), viewport, 10f)
        assertEquals(ImageRelease.RESTORE, session.release(viewport, -1400f, 0f, 1200f))
    }

    @Test
    fun zoomedImageClosesOnlyWhenDraggedOutwardAtVerticalEdge() {
        val centered = ImageViewerGestureSession(ImageTransform(2f))
        centered.move(sample(0f, 40f), viewport, 10f)
        assertEquals(ImageGestureMode.PAN, centered.mode)
        val edge = ImageViewerGestureSession(ImageTransform(2f, y = 200f))
        edge.move(sample(0f, 220f), viewport, 10f)
        assertEquals(ImageGestureMode.CLOSE, edge.mode)
        assertEquals(ImageRelease.CLOSE, edge.release(viewport, 0f, 0f, 400f))
    }

    @Test
    fun closingVelocityAdjustsReferenceHalfHeightThreshold() {
        val session = session(0f, 180f)
        assertEquals(ImageRelease.RESTORE, session.release(viewport, 0f, 0f, 400f))
        assertEquals(ImageRelease.CLOSE, session.release(viewport, 0f, 1000f, 400f))
        assertEquals(ImageRelease.RESTORE, session.release(viewport, 0f, -1000f, 400f))
    }

    private fun session(
        dx: Float,
        dy: Float,
    ): ImageViewerGestureSession {
        val session = ImageViewerGestureSession(ImageTransform())
        session.move(sample(dx, dy), viewport, 10f)
        return session
    }

    private fun sample(
        dx: Float,
        dy: Float,
    ): ImageGestureSample = ImageGestureSample(dx, dy, 1f, 150f, 200f, 1)
}
