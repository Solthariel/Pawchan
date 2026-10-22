package com.solthariel.pawchan.media.video

internal fun videoDurationLabel(milliseconds: Long): String =
    when {
        milliseconds > 0L -> videoTimeLabel(milliseconds)
        else -> "—:—"
    }

internal fun videoTimeLabel(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0L) / MILLIS_PER_SECOND
    val minutes = seconds / SECONDS_PER_MINUTE
    return "$minutes:${(seconds % SECONDS_PER_MINUTE).toString().padStart(2, '0')}"
}

private const val MILLIS_PER_SECOND = 1000L
private const val SECONDS_PER_MINUTE = 60L
