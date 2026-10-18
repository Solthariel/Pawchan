package com.solthariel.pawchan

import com.solthariel.pawchan.media.ImageGestureMode
import com.solthariel.pawchan.media.ImageGestureSample
import com.solthariel.pawchan.media.ImageRelease
import com.solthariel.pawchan.media.ImageTransform
import com.solthariel.pawchan.media.ImageViewerGestureSession
import com.solthariel.pawchan.media.ImageViewport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageViewerGestureSessionTest {
    private val viewport = ImageViewport(300f, 400f, 300f, 400f)

    @Test
    fun smallMovementDoesNotStealTaps() {
        val session = ImageViewerGestureSession(ImageTransform())
        assertFalse(session.move(sample(3f, 2f), viewport, 10f))
        assertEquals(ImageGestureMode.WAITING, session.mode)
    }

    @Test
    fun slowSwipePagesInBothDirections() {
        val forward = ImageViewerGestureSession(ImageTransform())
        forward.move(sample(-180f, 2f), viewport, 10f)
        assertEquals(ImageRelease.NEXT, forward.release(viewport, 0f, 0f, 800f))
        val backward = ImageViewerGestureSession(ImageTransform())
        backward.move(sample(180f, 2f), viewport, 10f)
        assertEquals(ImageRelease.PREVIOUS, backward.release(viewport, 0f, 0f, 800f))
    }

    @Test
    fun shortSlowSwipeRestoresButCommittedFlingPages() {
        val session = ImageViewerGestureSession(ImageTransform())
        session.move(sample(-40f, 0f), viewport, 10f)
        assertEquals(ImageRelease.RESTORE, session.release(viewport, -10f, 0f, 800f))
        assertEquals(ImageRelease.NEXT, session.release(viewport, -1000f, 0f, 800f))
        assertEquals(ImageRelease.RESTORE, session.release(viewport, 1000f, 0f, 800f))
    }

    @Test
    fun zoomedImagePansAndHandsOffToPagingAtTheEdge() {
        val session = ImageViewerGestureSession(ImageTransform(2f))
        session.move(sample(-60f, 1f), viewport, 10f)
        assertEquals(ImageGestureMode.PAN, session.mode)
        assertEquals(-60f, session.transform.x, 0f)
        session.move(sample(-140f, 1f), viewport, 10f)
        assertEquals(ImageGestureMode.PAGE, session.mode)
        assertEquals(-150f, session.transform.x, 0f)
        assertEquals(-50f, session.travelX, 0f)
    }

    @Test
    fun claimedVerticalCloseCannotTurnIntoPinch() {
        val session = ImageViewerGestureSession(ImageTransform())
        session.move(sample(1f, 220f), viewport, 10f)
        assertEquals(ImageRelease.CLOSE, session.release(viewport, 0f, 0f, 800f))
        assertTrue(session.move(sample(0f, 0f, 2), viewport, 10f))
        assertEquals(ImageGestureMode.CLOSE, session.mode)
        assertEquals(220f, session.travelY, 0f)
        assertEquals(ImageRelease.CLOSE, session.release(viewport, 0f, 0f, 800f))
    }

    @Test
    fun initialVerticalDriftCanBecomeHorizontalPagingButPageCannotBecomeClose() {
        val session = ImageViewerGestureSession(ImageTransform())
        session.move(sample(1f, 12f), viewport, 10f)
        assertEquals(ImageGestureMode.CLOSE, session.mode)
        session.move(sample(-180f, 1f), viewport, 10f)
        assertEquals(ImageGestureMode.PAGE, session.mode)
        assertEquals(ImageRelease.NEXT, session.release(viewport, 0f, 0f, 800f))
        session.move(sample(0f, 300f), viewport, 10f)
        assertEquals(ImageGestureMode.PAGE, session.mode)
    }

    private fun sample(
        dx: Float,
        dy: Float,
        pointers: Int = 1,
    ): ImageGestureSample = ImageGestureSample(dx, dy, 1f, 150f, 200f, pointers)
}
