package com.solthariel.pawchan.ffmpeg;

/** A texture update alone is not decoder output. No pixel-colour heuristic: black video is valid. */
final class VideoSurfaceFrameGate {
    private VideoSurfaceFrameGate() {}

    static boolean isConfirmed(long surfaceGeneration, long renderedGeneration) {
        return surfaceGeneration > 0L && surfaceGeneration == renderedGeneration;
    }
}
