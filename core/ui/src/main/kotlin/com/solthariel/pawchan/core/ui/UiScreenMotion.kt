package com.solthariel.pawchan.core.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Ordinary, post-click transitions only. No gesture progress or predictive-back handler. */
fun uiScreenEnter(
    forward: Boolean = true,
    tab: Boolean = false,
) = fadeIn(tween(SCREEN_MOTION_MS)) +
    if (tab) {
        androidx.compose.animation.EnterTransition.None
    } else {
        slideInHorizontally(tween(SCREEN_MOTION_MS, easing = FastOutSlowInEasing)) { width ->
            if (forward) width / SCREEN_SHIFT_PART else -width / SCREEN_SHIFT_PART
        }
    }

fun uiScreenExit(
    forward: Boolean = true,
    tab: Boolean = false,
) = fadeOut(tween(SCREEN_MOTION_MS)) +
    if (tab) {
        androidx.compose.animation.ExitTransition.None
    } else {
        slideOutHorizontally(tween(SCREEN_MOTION_MS, easing = FastOutSlowInEasing)) { width ->
            if (forward) -width / SCREEN_SHIFT_PART else width / SCREEN_SHIFT_PART
        }
    }

@Composable
fun <T> UiPageTransition(
    target: T,
    depth: (T) -> Int,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(targetState = target, modifier = modifier, transitionSpec = {
        val forward = depth(targetState) > depth(initialState)
        val sameLevel = depth(targetState) == depth(initialState)
        uiScreenEnter(forward, sameLevel).togetherWith(uiScreenExit(forward, sameLevel))
    }, label = "screen-transition") { active -> content(active) }
}

private const val SCREEN_MOTION_MS = 200
private const val SCREEN_SHIFT_PART = 8
