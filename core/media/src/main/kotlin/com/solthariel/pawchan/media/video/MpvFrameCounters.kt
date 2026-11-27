package com.solthariel.pawchan.media.video

import io.github.yuroyami.libmpvkt.jni.MpvNative
import io.github.yuroyami.libmpvkt.jni.MpvNativeApi

/** Optional, worker-thread samples. Never turn an unavailable backend counter into zero. */
internal data class MpvFrameCounters(
    val position: Long,
    val decoderDrops: Long?,
    val outputDrops: Long?,
    val hardware: Boolean?,
)

@OptIn(MpvNativeApi::class)
internal fun readMpvFrameCounters(
    handle: Long,
    sample: MpvPollSample,
): MpvFrameCounters = mpvFrameCounters(sample) { name -> MpvNative.getPropertyString(handle, name) }

internal fun mpvFrameCounters(
    sample: MpvPollSample,
    property: (String) -> String?,
): MpvFrameCounters {
    fun count(name: String): Long? = property(name)?.toLongOrNull()?.takeIf { it >= 0 }
    return MpvFrameCounters(
        position = sample.position,
        decoderDrops = count("decoder-frame-drop-count"),
        outputDrops = count("frame-drop-count"),
        hardware = sample.hardwareDecoded,
    )
}
