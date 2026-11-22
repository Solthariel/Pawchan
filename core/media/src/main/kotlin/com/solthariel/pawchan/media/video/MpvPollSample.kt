package com.solthariel.pawchan.media.video

import io.github.yuroyami.libmpvkt.jni.MpvNative
import io.github.yuroyami.libmpvkt.jni.MpvNativeApi

/** Snapshot collected on the mpv worker; no Android UI state is read here. */
internal data class MpvPollSample(
    val timing: MpvPollTiming,
    val hasFile: Boolean,
    val paused: Boolean,
    val buffering: Boolean,
    val aspect: Double?,
    val decoder: String?,
    val facts: Map<String, String>,
) {
    val position: Long get() = timing.position
    val duration: Long get() = timing.duration
    val hardwareDecoded: Boolean? get() = reportedHardwareDecoder(hasFile, decoder)
}

internal data class MpvPollTiming(
    val position: Long,
    val duration: Long,
)

internal fun reportedHardwareDecoder(
    hasFile: Boolean,
    decoder: String?,
): Boolean? = if (!hasFile || decoder == null) null else decoder != "no"

@OptIn(MpvNativeApi::class)
internal fun readMpvPoll(
    handle: Long,
    previous: MpvPollSample? = null,
): MpvPollSample {
    fun property(name: String): String? = MpvNative.getPropertyString(handle, name)

    fun seconds(name: String): Long {
        val value = property(name)?.toDoubleOrNull() ?: 0.0
        return (value * MILLIS_PER_SECOND).toLong().coerceAtLeast(0)
    }
    val position = seconds("time-pos")
    val duration = seconds("duration")
    val paused = property("pause") == "yes"
    val buffering = property("paused-for-cache") == "yes"
    val hasFile = property("path") != null
    // Keep timeline and play/pause responsive, but read slow-changing codec/metadata
    // properties only on opening and periodically while a file is active.
    val details = previous?.takeIf { it.hasFile && hasFile }
    val aspect =
        when {
            !hasFile -> {
                null
            }

            details != null -> {
                details.aspect
            }

            else -> {
                mpvDisplayAspect(
                    property("video-params/aspect")?.toDoubleOrNull(),
                    property("video-params/rotate")?.toIntOrNull(),
                )
            }
        }
    val decoder = if (!hasFile) null else details?.decoder ?: property("hwdec-current")
    val facts =
        when {
            !hasFile -> emptyMap()
            details != null -> details.facts
            else -> readMpvFacts(::property)
        }
    return MpvPollSample(MpvPollTiming(position, duration), hasFile, paused, buffering, aspect, decoder, facts)
}

private fun readMpvFacts(property: (String) -> String?): Map<String, String> {
    val properties =
        mapOf(
            "width" to "video-params/w",
            "height" to "video-params/h",
            "frame_rate" to "container-fps",
            "video_format" to "video-codec",
            "decoder_name" to "video-decoder",
            "audio_format" to "current-tracks/audio/codec",
            "sample_rate" to "audio-params/samplerate",
            "channels" to "audio-params/channel-count",
            "title" to "metadata/title",
            "encoder" to "metadata/encoder",
        )
    return properties.mapNotNull { (key, name) -> property(name)?.let { key to it } }.toMap()
}

private const val MILLIS_PER_SECOND = 1000.0

internal fun pollPlaybackStage(
    ended: Boolean,
    hasFile: Boolean,
    buffering: Boolean,
): VideoStage = if (ended || (hasFile && !buffering)) VideoStage.READY else VideoStage.BUFFERING
