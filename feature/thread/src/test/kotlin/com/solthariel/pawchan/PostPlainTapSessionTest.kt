package com.solthariel.pawchan

import androidx.compose.ui.geometry.Offset
import com.solthariel.pawchan.thread.EXTERNAL_TAG
import com.solthariel.pawchan.thread.PostTextGestureSession
import com.solthariel.pawchan.thread.PostTextGestureTiming
import org.junit.Assert.assertEquals
import org.junit.Test

class PostPlainTapSessionTest {
    @Test
    fun secondPlainTapSelectsWithoutDelayingFirstTap() {
        val h = PostGestureHarness(null)
        h.session.down(Offset.Zero, 0)
        h.session.up(20)
        assertEquals(1, h.clicks)
        h.session.down(Offset.Zero, 100)
        h.session.up(120)
        assertEquals(1, h.selections)
        assertEquals(1, h.clicks)
    }

    @Test
    fun tooEarlyTooLateAndDistantTapsDoNotSelect() {
        for ((time, position) in listOf(30L to Offset.Zero, 500L to Offset.Zero, 100L to Offset(30f, 0f))) {
            val h = PostGestureHarness(null)
            h.session.down(Offset.Zero, 0)
            h.session.up(20)
            h.session.down(position, time)
            h.session.up(time + 20)
            assertEquals(0, h.selections)
            assertEquals(2, h.clicks)
        }
    }

    @Test
    fun draggingPlainTextCancelsAndResetsDoubleTapSequence() {
        val h = PostGestureHarness(null)
        h.session.down(Offset.Zero, 0)
        h.session.up(20)
        h.session.down(Offset.Zero, 100)
        h.session.move(Offset(20f, 0f))
        h.session.up(120)
        h.session.down(Offset.Zero, 180)
        h.session.up(200)
        assertEquals(0, h.selections)
        assertEquals(2, h.clicks)
    }

    @Test
    fun plainLongPressShowsPostMenuAndDoesNotSelectOnNextTap() {
        val h = PostGestureHarness(null)
        h.session.down(Offset.Zero, 0)
        h.session.longPress()
        h.session.up(600)
        h.session.down(Offset.Zero, 650)
        h.session.up(680)
        assertEquals(1, h.menus)
        assertEquals(1, h.clicks)
        assertEquals(0, h.selections)
    }

    @Test
    fun pressCapturesOwnerUntilReleaseEvenWhenCallbacksChange() {
        val first = PostGestureHarness(EXTERNAL_TAG)
        val second = PostGestureHarness(EXTERNAL_TAG)
        var owner = first.actions
        val session = PostTextGestureSession({ owner }, 8f, PostTextGestureTiming(40, 300, 16f))
        session.down(Offset.Zero, 0)
        owner = second.actions
        session.up(20)
        assertEquals(1, first.clicks)
        assertEquals(0, second.clicks)
        assertEquals(false, first.pressed)
        session.down(Offset.Zero, 100)
        session.up(120)
        assertEquals(1, second.clicks)
    }
}
