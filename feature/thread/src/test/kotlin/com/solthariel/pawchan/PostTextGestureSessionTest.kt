package com.solthariel.pawchan

import androidx.compose.ui.geometry.Offset
import com.solthariel.pawchan.thread.EXTERNAL_TAG
import com.solthariel.pawchan.thread.PostTextGestureSession
import com.solthariel.pawchan.thread.PostTextGestureTiming
import com.solthariel.pawchan.thread.PostTextGestures
import com.solthariel.pawchan.thread.PostTextTarget
import com.solthariel.pawchan.thread.SPOILER_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PostTextGestureSessionTest {
    @Test
    fun linkUpDispatchesImmediatelyAndRepeatedTapsNeverSelectText() {
        val h = PostGestureHarness(EXTERNAL_TAG)
        h.session.down(Offset.Zero, 0)
        assertTrue(h.pressed)
        h.session.up(30)
        assertEquals(1, h.clicks)
        assertFalse(h.pressed)
        h.session.down(Offset.Zero, 80)
        h.session.up(100)
        assertEquals(2, h.clicks)
        assertEquals(0, h.selections)
    }

    @Test
    fun spoilerDoesNotScheduleLongPressOrParticipateInDoubleTap() {
        val h = PostGestureHarness(SPOILER_TAG)
        h.session.down(Offset.Zero, 0)
        assertFalse(h.session.acceptsLongPress)
        h.session.longPress()
        h.session.up(1000)
        assertEquals(1, h.clicks)
        assertEquals(0, h.menus)
        assertEquals(0, h.selections)
    }

    @Test
    fun longPressClearsFeedbackAndFollowingUpCannotOpenLink() {
        val h = PostGestureHarness(EXTERNAL_TAG)
        h.session.down(Offset.Zero, 0)
        h.session.longPress()
        assertFalse(h.pressed)
        assertFalse(h.session.isActive)
        h.session.up(600)
        assertEquals(1, h.menus)
        assertEquals(0, h.clicks)
    }

    @Test
    fun leavingSlopAndOriginalTargetCancelsWithoutRearming() {
        val h = PostGestureHarness(EXTERNAL_TAG)
        h.session.down(Offset.Zero, 0)
        h.session.move(Offset(100f, 0f))
        assertFalse(h.pressed)
        h.session.move(Offset.Zero)
        h.session.up(50)
        assertEquals(0, h.clicks)
    }

    @Test
    fun movementWithinSlopOrWithinOriginalTargetKeepsPress() {
        val h = PostGestureHarness(EXTERNAL_TAG)
        h.session.down(Offset(39f, 0f), 0)
        h.session.move(Offset(41f, 0f))
        assertTrue(h.session.isActive)
        h.session.move(Offset(10f, 0f))
        assertTrue(h.session.isActive)
        h.session.up(50)
        assertEquals(1, h.clicks)
    }

    @Test
    fun systemCancelClearsFeedbackAndPreventsBothMenuAndClick() {
        val h = PostGestureHarness(EXTERNAL_TAG)
        h.session.down(Offset.Zero, 0)
        h.session.cancel()
        assertFalse(h.pressed)
        h.session.longPress()
        h.session.up(600)
        assertEquals(0, h.menus)
        assertEquals(0, h.clicks)
    }

    @Test
    fun releaseWithoutMoveStillChecksTargetWhenAdapterReportsPosition() {
        val h = PostGestureHarness(EXTERNAL_TAG)
        h.session.down(Offset.Zero, 0)
        h.session.move(Offset(100f, 100f))
        h.session.up(30)
        assertEquals(0, h.clicks)
    }
}

internal class PostGestureHarness(
    tag: String?,
) {
    var clicks = 0
    var selections = 0
    var menus = 0
    var pressed = false
    private val target = tag?.let { PostTextTarget(it, "target", 0, 4) }
    val actions =
        PostTextGestures(
            onTap = { clicks++ },
            onDoubleTap = { selections++ },
            onLongPress = { menus++ },
            onPress = { pressed = true },
            onRelease = { pressed = false },
            targetAt = { if (it.x in 0f..40f && it.y in 0f..40f) target else null },
        )
    val session = PostTextGestureSession({ actions }, 8f, PostTextGestureTiming(40, 300, 16f))
}
