package com.solthariel.pawchan.media.video

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoEdgeProgressTest {
    @Test fun onlyHiddenControlsWithAKnownTimelineShowTheIndicator() {
        val state = VideoPlaybackStatus(stage = VideoStage.READY, timeline = VideoTimeline(25, 100))
        assertTrue(showVideoEdgeProgress(state, false))
        assertFalse(showVideoEdgeProgress(state, true))
        assertFalse(showVideoEdgeProgress(state.copy(timeline = VideoTimeline()), false))
        assertFalse(showVideoEdgeProgress(state.copy(stage = VideoStage.ERROR), false))
        assertTrue(showVideoEdgeProgress(state.copy(stage = VideoStage.BUFFERING), false))
    }

    @Test fun progressClampsInvalidPositionsAndHandlesUnknownDuration() {
        assertEquals(0.25f, videoEdgeProgressFraction(VideoTimeline(25, 100)))
        assertEquals(0f, videoEdgeProgressFraction(VideoTimeline(-25, 100)))
        assertEquals(1f, videoEdgeProgressFraction(VideoTimeline(125, 100)))
        assertEquals(0f, videoEdgeProgressFraction(VideoTimeline(25, 0)))
        assertEquals(0f, videoEdgeProgressFraction(VideoTimeline(25, -1)))
        assertEquals(1f, videoEdgeProgressFraction(VideoTimeline(Long.MAX_VALUE, Long.MAX_VALUE)))
    }

    @Test fun bothThemesReuseTheSeekPaletteWithQuieterOpacity() {
        for (colors in listOf(lightColorScheme(primary = Color.Red), darkColorScheme(primary = Color.Green))) {
            assertEquals(colors.primary.copy(alpha = 0.65f), videoEdgeActiveColor(colors))
            assertEquals(colors.onSurfaceVariant.copy(alpha = 0.25f), videoEdgeInactiveColor(colors))
        }
    }
}
