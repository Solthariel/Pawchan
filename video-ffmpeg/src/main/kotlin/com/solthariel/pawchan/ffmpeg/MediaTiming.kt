package com.solthariel.pawchan.ffmpeg

import android.util.Log
import java.util.function.Consumer
import java.util.function.LongSupplier

/** Read the opt-in flag once per operation; no global registry, URLs or exception messages. */
object MediaTiming {
    private const val TAG = "PawchanMediaTiming"

    @JvmStatic
    fun enabled(): Boolean = Log.isLoggable(TAG, Log.DEBUG)

    @JvmStatic
    fun create(): TimingProbe =
        TimingProbe(
            enabled(),
            LongSupplier { System.nanoTime() },
            Consumer { line ->
                Log.d(TAG, line)
                Unit
            },
        )
}
