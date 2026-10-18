package com.solthariel.pawchan.ffmpeg;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class VideoSurfaceFrameGateTest {
    @Test public void textureWithoutPostedDecodeIsNotReady() {
        assertFalse(VideoSurfaceFrameGate.isConfirmed(1, 0));
    }
    @Test public void olderSurfaceCannotConfirmReplacement() {
        assertFalse(VideoSurfaceFrameGate.isConfirmed(2, 1));
    }
    @Test public void futureSurfaceCannotConfirmDetachedTexture() {
        assertFalse(VideoSurfaceFrameGate.isConfirmed(1, 2));
    }
    @Test public void missingSurfaceIsNotReady() {
        assertFalse(VideoSurfaceFrameGate.isConfirmed(0, 0));
        assertFalse(VideoSurfaceFrameGate.isConfirmed(-1, -1));
    }
    @Test public void matchingPostedDecodeConfirmsEvenRetainedSurface() {
        assertTrue(VideoSurfaceFrameGate.isConfirmed(1, 1));
        assertTrue(VideoSurfaceFrameGate.isConfirmed(4294967296L, 4294967296L));
    }
}
