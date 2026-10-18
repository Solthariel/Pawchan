package com.solthariel.pawchan.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

@Composable
fun UiRefreshButton(
    isRefreshing: Boolean,
    onClick: () -> Unit,
    description: String,
    enabled: Boolean = true,
) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            rotateWhileRefreshing(rotation)
        } else {
            settleRefreshRotation(rotation)
        }
    }
    UiToolbarButton(
        kind = UiIconKind.REFRESH,
        description = description,
        rotationDegrees = rotation.value,
        enabled = enabled,
        onClick = onClick,
    )
}

private suspend fun rotateWhileRefreshing(rotation: Animatable<Float, AnimationVector1D>) {
    while (currentCoroutineContext().isActive) {
        rotation.animateTo(
            targetValue = rotation.value + FULL_ROTATION_DEGREES,
            animationSpec = tween(REFRESH_ROTATION_DURATION_MS, easing = LinearEasing),
        )
    }
}

private suspend fun settleRefreshRotation(rotation: Animatable<Float, AnimationVector1D>) {
    val normalizedRotation = rotation.value % FULL_ROTATION_DEGREES
    val remainingDegrees = FULL_ROTATION_DEGREES - normalizedRotation
    refreshSettleDuration(remainingDegrees)?.let { duration ->
        rotation.animateTo(
            targetValue = rotation.value + remainingDegrees,
            animationSpec = tween(duration, easing = LinearEasing),
        )
    }
    rotation.snapTo(0f)
}

internal fun refreshSettleDuration(remainingDegrees: Float): Int? =
    if (remainingDegrees in MINIMUM_SETTLE_DEGREES..<FULL_ROTATION_DEGREES) {
        (REFRESH_ROTATION_DURATION_MS * remainingDegrees / FULL_ROTATION_DEGREES).roundToInt()
    } else {
        null
    }

private const val FULL_ROTATION_DEGREES = 360f
private const val MINIMUM_SETTLE_DEGREES = 0.5f
private const val REFRESH_ROTATION_DURATION_MS = 850
