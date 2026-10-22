package com.solthariel.pawchan.ffmpeg;

import android.view.Surface;

/** The only JNI boundary. Handles are owned by one PawPlayback and destroyed after its pump exits. */
final class PawNative {
    static final int PLAY = 1, SEEK = 2, SPEED = 3, INTERRUPT = 4;
    private PawNative() {}
    static native long create();
    static native boolean open(long handle, PublishedInput input, PcmOutput audio, boolean software);
    static native String[] metadata(long handle);
    static native int pump(long handle);
    static native void command(long handle, int command, long value);
    static native long[] status(long handle);
    static native void surface(long handle, Surface surface);
    static native void destroy(long handle);
}
