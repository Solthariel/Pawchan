package com.solthariel.pawchan.ffmpeg;
import android.view.Surface;
/** Synchronizes short JNI controls; open/pump/release stay on one decoder thread. */
final class PlaybackHandle {
    private long value;
    synchronized void allocate() { value = PawNative.create(); if (value == 0) throw new IllegalStateException("Allocation failed"); }
    boolean open(PublishedInput input, PcmOutput audio, boolean software) { return PawNative.open(value, input, audio, software); }
    int pump() { return PawNative.pump(value); }
    synchronized long[] status() { return value == 0 ? new long[6] : PawNative.status(value); }
    synchronized String[] metadata() { return value == 0 ? new String[0] : PawNative.metadata(value); }
    synchronized void command(int command, long data) { if (value != 0) PawNative.command(value, command, data); }
    synchronized void surface(Surface surface) { if (value != 0) PawNative.surface(value, surface); }
    synchronized void dispose() { if (value != 0) { PawNative.destroy(value); value = 0; } }
}
