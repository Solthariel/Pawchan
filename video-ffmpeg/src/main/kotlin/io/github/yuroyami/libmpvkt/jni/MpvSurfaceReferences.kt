package io.github.yuroyami.libmpvkt.jni

import android.view.Surface

/** JNI global references passed to libmpv as Android window handles. */
@MpvNativeApi
public object MpvSurfaceReferences {
    init {
        System.loadLibrary("mpv")
        System.loadLibrary("mpvkt_jni")
    }

    /** Keep the reference until mpv is done with the surface, then call [release]. */
    public external fun create(surface: Surface): Long

    /** Release a reference returned by [create]; zero is a no-op. */
    public external fun release(reference: Long)
}
