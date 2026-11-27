package com.solthariel.pawchan

import com.solthariel.pawchan.media.video.PlaybackFocusGate
import com.solthariel.pawchan.media.video.VideoSoundState
import com.solthariel.pawchan.media.video.needsFocus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoAudioFocusGateTest {
    @Test fun mutedPlaybackAndSilentTracksNeverCompeteWithAnotherMusicApp() {
        var requests = 0
        var abandons = 0
        val gate =
            PlaybackFocusGate({
                requests++
                true
            }, { abandons++ })
        val audible = VideoSoundState(hasAudio = true)
        val muted = audible.copy(muted = true)
        repeat(5) {
            gate.update(muted.needsFocus(playing = true))
            gate.update(audible.copy(hasAudio = false).needsFocus(playing = true))
            gate.update(audible.needsFocus(playing = false))
        }
        assertEquals(0, requests)
        assertEquals(0, abandons)
        repeat(4) { gate.update(audible.needsFocus(true)) }
        assertTrue(gate.isHeld)
        repeat(4) { gate.update(muted.needsFocus(true)) }
        assertFalse(gate.isHeld)
        assertEquals(1, requests)
        assertEquals(1, abandons)
        gate.update(audible.needsFocus(true))
        gate.update(audible.needsFocus(false))
        assertEquals(2, requests)
        assertEquals(2, abandons)
    }

    @Test fun deniedRequestIsNeverTreatedAsOwnedOrAbandoned() {
        var granted = false
        var abandons = 0
        val gate = PlaybackFocusGate({ granted }, { abandons++ })
        assertFalse(gate.update(true))
        assertFalse(gate.isHeld)
        assertTrue(gate.update(false))
        assertEquals(0, abandons)
        granted = true
        assertTrue(gate.update(true))
        assertTrue(gate.update(true))
        assertTrue(gate.update(false))
        assertEquals(1, abandons)
    }
}
