package com.solthariel.pawchan.media.video

/** Match mpv-android's video aspect calculation, including metadata rotation. */
internal fun mpvDisplayAspect(
    aspect: Double?,
    rotation: Int?,
): Double? {
    if (aspect == null || !aspect.isFinite() || aspect <= 0.0) return null
    val normalized = Math.floorMod(rotation ?: 0, FULL_ROTATION_DEGREES)
    val displayed =
        when (normalized) {
            QUARTER_TURN_DEGREES, THREE_QUARTER_TURN_DEGREES -> 1.0 / aspect
            else -> aspect
        }
    return displayed.takeIf { it.isFinite() && it > 0.0 }
}

/** Prefer the actual decoder aspect; metadata can be absent or malformed. */
internal fun videoPipAspect(
    actual: Double?,
    width: Int?,
    height: Int?,
): Double {
    val dimensions =
        if (width != null && height != null && positiveDimensions(width, height)) {
            width.toDouble() / height
        } else {
            null
        }
    val ratio = actual?.takeIf { it.isFinite() && it > 0.0 } ?: dimensions ?: DEFAULT_VIDEO_ASPECT
    return ratio.coerceIn(1.0 / MAX_PIP_ASPECT, MAX_PIP_ASPECT)
}

private fun positiveDimensions(
    width: Int,
    height: Int,
): Boolean = width > 0 && height > 0

private const val FULL_ROTATION_DEGREES = 360
private const val QUARTER_TURN_DEGREES = 90
private const val THREE_QUARTER_TURN_DEGREES = 270
private const val DEFAULT_VIDEO_ASPECT = 16.0 / 9.0
private const val MAX_PIP_ASPECT = 2.35
