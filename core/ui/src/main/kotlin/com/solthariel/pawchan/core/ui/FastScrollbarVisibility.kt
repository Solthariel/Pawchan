package com.solthariel.pawchan.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

@Composable
internal fun rememberFastScrollbarReveal(
    available: Boolean,
    active: Boolean,
    interaction: Int,
): Animatable<Float, AnimationVector1D> {
    val reveal = remember { Animatable(0f) }
    var handledInteraction by remember { mutableIntStateOf(0) }
    LaunchedEffect(available, active, interaction) {
        val interacted = interaction != handledInteraction
        handledInteraction = interaction
        if (!available) {
            reveal.snapTo(0f)
            return@LaunchedEffect
        }
        // A tap can begin and end before composition observes the pressed state.
        val finishShow = reveal.value < 1f && reveal.targetValue > 0f
        if (active || interacted || finishShow) {
            reveal.animateTo(1f, tween(FAST_SCROLL_SHOW_MS))
        }
        if (!active && reveal.value > 0f) {
            delay(FAST_SCROLL_HIDE_DELAY_MS)
            reveal.animateTo(0f, tween(FAST_SCROLL_HIDE_MS))
        }
    }
    return reveal
}

internal const val FAST_SCROLL_SHOW_MS = 100
internal const val FAST_SCROLL_HIDE_MS = 200
internal const val FAST_SCROLL_HIDE_DELAY_MS = 1000L
