package com.solthariel.pawchan

import com.solthariel.pawchan.media.thumbnail.THUMBNAIL_FADE_MILLIS
import com.solthariel.pawchan.media.thumbnail.ThumbnailFadeEasing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThumbnailFadeEasingTest {
    @Test
    fun fadeHasDeceleratingCurveAndExactEndpoints() {
        assertEquals(200, THUMBNAIL_FADE_MILLIS)
        assertEquals(0f, ThumbnailFadeEasing.transform(0f), 0f)
        assertEquals(0.75f, ThumbnailFadeEasing.transform(0.5f), 0f)
        assertEquals(1f, ThumbnailFadeEasing.transform(1f), 0f)
        assertEquals(0f, ThumbnailFadeEasing.transform(-1f), 0f)
        assertEquals(1f, ThumbnailFadeEasing.transform(2f), 0f)
    }

    @Test
    fun alphaNeverMovesBackwardOrOvershoots() {
        var previous = 0f
        for (step in 0..200) {
            val alpha = ThumbnailFadeEasing.transform(step / 200f)
            assertTrue(alpha >= previous)
            assertTrue(alpha in 0f..1f)
            previous = alpha
        }
    }
}
