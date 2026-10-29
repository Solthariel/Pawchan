package com.solthariel.pawchan.ffmpeg;

import android.util.Log;

/** Read the opt-in flag once per operation; no global registry, URLs or exception messages. */
public final class MediaTiming {
    private static final String TAG = "PawchanMediaTiming";
    private MediaTiming() {}
    public static boolean enabled() { return Log.isLoggable(TAG, Log.DEBUG); }
    public static TimingProbe create() {
        return new TimingProbe(enabled(), System::nanoTime, line -> Log.d(TAG, line));
    }
}
