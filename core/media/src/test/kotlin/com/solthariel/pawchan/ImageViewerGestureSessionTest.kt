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
    fun shortVerticalDriftWaitsAndASidewaysSwipePagesButPageCannotBecomeClose() {
        val session = ImageViewerGestureSession(ImageTransform())
        assertTrue(session.move(sample(1f, 12f), viewport, 10f))
        assertEquals(ImageGestureMode.WAITING, session.mode)
        session.move(sample(-180f, 1f), viewport, 10f)
        assertEquals(ImageGestureMode.PAGE, session.mode)
        assertEquals(ImageRelease.NEXT, session.release(viewport, 0f, 0f, 800f))
        session.move(sample(0f, 300f), viewport, 10f)
        assertEquals(ImageGestureMode.PAGE, session.mode)
    }

    /** Thumb arc: starts downward, then sweeps right. It used to close the viewer, now it pages. */
    @Test
    fun thumbArcToTheRightPagesInsteadOfClosing() {
        val waiting = ImageViewerGestureSession(ImageTransform())
        waiting.move(sample(8f, 20f), viewport, 10f)
        assertEquals(ImageGestureMode.WAITING, waiting.mode)
        waiting.move(sample(60f, 15f), viewport, 10f)
        assertEquals(ImageGestureMode.PAGE, waiting.mode)
        assertEquals(ImageRelease.PREVIOUS, waiting.release(viewport, 1200f, 300f, 400f))
        val claimed = ImageViewerGestureSession(ImageTransform())
        claimed.move(sample(2f, 40f), viewport, 10f)
        assertEquals(ImageGestureMode.CLOSE, claimed.mode)
        claimed.move(sample(90f, 5f), viewport, 10f)
        assertEquals(ImageGestureMode.PAGE, claimed.mode)
        assertEquals(ImageRelease.PREVIOUS, claimed.release(viewport, 1200f, 0f, 400f))
    }

    @Test
    fun diagonalSwipeBetweenFortyFiveAndSixtyThreeDegreesPages() {
        val session = ImageViewerGestureSession(ImageTransform())
        session.move(sample(-40f, 60f), viewport, 10f)
        assertEquals(ImageGestureMode.PAGE, session.mode)
    }

    /** A short fast flick must never close: the threshold has a floor of 12% of the height. */
    @Test
    fun shortFastJitterDoesNotClose() {
        val session = ImageViewerGestureSession(ImageTransform())
        session.move(sample(2f, 14f), viewport, 10f)
        session.move(sample(-1f, 12f), viewport, 10f)
        assertEquals(ImageGestureMode.CLOSE, session.mode)
        assertEquals(ImageRelease.RESTORE, session.release(viewport, 0f, 8000f, 400f))
    }

    @Test
    fun honestSwipeDownStillCloses() {
        val slow = ImageViewerGestureSession(ImageTransform())
        slow.move(sample(3f, 210f), viewport, 10f)
        assertEquals(ImageRelease.CLOSE, slow.release(viewport, 0f, 0f, 400f))
        val fast = ImageViewerGestureSession(ImageTransform())
        fast.move(sample(4f, 60f), viewport, 10f)
        assertEquals(ImageRelease.CLOSE, fast.release(viewport, 0f, 3000f, 400f))
    }

    private fun sample(
        dx: Float,
        dy: Float,
        pointers: Int = 1,
    ): ImageGestureSample = ImageGestureSample(dx, dy, 1f, 150f, 200f, pointers)
}
