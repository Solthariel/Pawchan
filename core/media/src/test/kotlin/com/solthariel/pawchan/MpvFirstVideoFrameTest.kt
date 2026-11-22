package com.solthariel.pawchan

import com.solthariel.pawchan.media.video.MpvFirstVideoFrame
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MpvFirstVideoFrameTest {
    private val output = MpvFirstVideoFrame<Any>()
    private val first = Any()
    private val replacement = Any()

    @Test fun rendererClearIsNotTheFirstMediaFrame() {
        output.mount(first)
        assertFalse(output.onTextureUpdate(first))
        assertTrue(output.playbackRestarted())
        assertTrue(output.onTextureUpdate(first))
        assertFalse(output.onTextureUpdate(first))
    }

    @Test fun fileCanBeReadyBeforeItsFirstTextureIsMounted() {
        assertTrue(output.playbackRestarted())
        assertFalse(output.onTextureUpdate(first))
        output.mount(first)
        assertTrue(output.onTextureUpdate(first))
    }

    @Test fun repeatMountOfSameSurfaceDoesNotPublishAnExtraFrame() {
        output.mount(first)
        output.playbackRestarted()
        assertTrue(output.onTextureUpdate(first))
        output.mount(first)
        assertFalse(output.onTextureUpdate(first))
    }

    @Test fun aSeekDoesNotAnnounceASecondFirstFrame() {
        output.mount(first)
        output.playbackRestarted()
        assertTrue(output.onTextureUpdate(first))
        assertFalse(output.playbackRestarted())
        assertFalse(output.onTextureUpdate(first))
    }

    @Test fun replacingAPausedSurfaceRejectsOldCallbacksButDoesNotRequireANewFile() {
        output.mount(first)
        output.playbackRestarted()
        output.mount(null)
        assertFalse(output.onTextureUpdate(first))
        output.mount(replacement)
        assertFalse(output.onTextureUpdate(first))
        assertTrue(output.onTextureUpdate(replacement))
    }

    @Test fun reopeningOnTheSameTextureRequiresTheNewPlaybackRestart() {
        output.mount(first)
        output.playbackRestarted()
        output.resetPlayback()
        output.mount(first)
        assertFalse(output.onTextureUpdate(first))
        assertTrue(output.playbackRestarted())
        assertTrue(output.onTextureUpdate(first))
    }
}
