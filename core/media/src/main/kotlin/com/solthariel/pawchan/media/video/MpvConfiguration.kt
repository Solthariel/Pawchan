package com.solthariel.pawchan.media.video

import io.github.yuroyami.libmpvkt.jni.MpvNative
import io.github.yuroyami.libmpvkt.jni.MpvNativeApi

/** Configure libmpv before initialization, on the engine's worker thread. */
@OptIn(MpvNativeApi::class)
internal fun configureMpvOptions(
    h: Long,
    software: Boolean,
) {
    val options =
        listOf(
            "profile" to "fast",
            "gpu-context" to "android",
            "opengl-es" to "yes",
            "vo" to "null",
            "force-window" to "no",
            "ao" to "audiotrack,opensles",
            "idle" to "yes",
            "pause" to "yes",
            "hwdec" to if (software) "no" else "auto-safe",
            "hwdec-codecs" to "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1",
            "demuxer-max-bytes" to "67108864",
            "demuxer-max-back-bytes" to "67108864",
        )
    for ((name, value) in options) {
        check(MpvNative.setOptionString(h, name, value) >= 0) { "mpv option: $name" }
    }
}
