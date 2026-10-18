package com.solthariel.pawchan.media.video

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos

/**
 * A new owner or output reset gets an opaque poster during composition, before any effect runs.
 * Only a rendered output may fade it out. The short hold is a presentation cushion, not a decoder timer.
 */
@Composable
internal fun rememberVideoPosterAlpha(
    hasVideoOutput: Boolean,
    owner: Any = Unit,
): State<Float> {
    val alpha = remember(owner, hasVideoOutput) { Animatable(1f) }
    LaunchedEffect(alpha, hasVideoOutput) {
        if (hasVideoOutput) {
            repeat(POSTER_HOLD_FRAMES) { withFrameNanos { } }
            alpha.animateTo(0f, tween(POSTER_FADE_MS, easing = LinearEasing))
        }
    }
    return alpha.asState()
}

/** Empty surfaces remain hidden independently of the availability of their thumbnail. */
internal fun videoSurfaceAlpha(hasVideoOutput: Boolean): Float = if (hasVideoOutput) 1f else 0f

internal const val POSTER_HOLD_FRAMES = 2
internal const val POSTER_FADE_MS = 90
