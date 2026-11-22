package com.solthariel.pawchan.core.ui

import kotlin.math.PI
import kotlin.math.cos

/** Follow the finger, clamping overpull at the loading animation's matching endpoint. */
internal fun pullRefreshMorphPhase(strain: Float): Float = strain.coerceIn(0f, 1f)

/** Bounded radius with matching phase endpoints, shared by both pull-to-refresh sides. */
internal fun refreshMorphRadius(
    angle: Double,
    phase: Float,
): Float {
    val blend = (1.0 - cos(phase * 2.0 * PI)) / 2.0
    val waves = (1.0 - blend) * cos(WAVY_LOBES * angle) + blend * cos(SQUARE_LOBES * angle)
    return (BASE_RADIUS + WAVE_AMPLITUDE * waves).toFloat()
}

private const val WAVY_LOBES = 8.0
private const val SQUARE_LOBES = 4.0
private const val BASE_RADIUS = 0.88
private const val WAVE_AMPLITUDE = 0.12
