package com.solthariel.pawchan.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal class PullRefreshVisual {
    val strain = Animatable(0f)
    val scale = Animatable(1f)
    val rotation = Animatable(0f)
    var appliedRelease by mutableIntStateOf(-1)
    var spinning by mutableStateOf(false)
    var spinStartAngle by mutableFloatStateOf(0f)
}

@Composable
internal fun rememberPullRefreshVisual(release: PullRefreshRelease): PullRefreshVisual {
    val visual = remember { PullRefreshVisual() }
    LaunchedEffect(release) {
        when (release.mode) {
            PullRefreshReleaseMode.LOAD -> visual.startLoading(release)
            PullRefreshReleaseMode.RETRACT -> visual.retract(release)
            PullRefreshReleaseMode.FINISH -> visual.finishLoading(release)
        }
    }
    LaunchedEffect(visual.spinning) {
        if (visual.spinning) {
            while (true) {
                visual.rotation.snapTo(0f)
                visual.rotation.animateTo(1f, tween(SPIN_CYCLE_MILLIS, easing = LinearEasing))
            }
        }
    }
    return visual
}

private suspend fun PullRefreshVisual.startLoading(release: PullRefreshRelease) {
    strain.snapTo(release.strain)
    scale.snapTo(1f)
    appliedRelease = release.generation
    spinStartAngle = pullRefreshArc(release.strain, null).first
    spinning = true
    strain.animateTo(1f, tween(PULL_FOLD_MILLIS, easing = LinearEasing))
}

private suspend fun PullRefreshVisual.retract(release: PullRefreshRelease) {
    spinning = false
    scale.snapTo(1f)
    strain.snapTo(release.strain)
    appliedRelease = release.generation
    if (release.strain > 0f) strain.animateTo(0f, tween(PULL_FOLD_MILLIS, easing = LinearEasing))
}

private suspend fun PullRefreshVisual.finishLoading(release: PullRefreshRelease) =
    coroutineScope {
        appliedRelease = release.generation
        launch { strain.animateTo(1f, tween(PULL_FOLD_MILLIS, easing = LinearEasing)) }
        scale.animateTo(0f, tween(LOADING_FOLD_MILLIS, easing = LinearEasing))
        strain.snapTo(0f)
        spinning = false
    }

private const val PULL_FOLD_MILLIS = 100
private const val LOADING_FOLD_MILLIS = 150
private const val SPIN_CYCLE_MILLIS = 1_333
