package com.solthariel.pawchan.media.video

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext
import com.solthariel.pawchan.media.ImageViewerMotion
import kotlinx.coroutines.delay

internal class VideoChromeActivity {
    var touching by mutableStateOf(false)
        private set
    var revision by mutableIntStateOf(0)
        private set

    fun onPointer(pressed: Boolean) {
        if (pressed || touching) revision++
        touching = pressed
    }
}

@Composable
internal fun rememberVideoChromeActivity(
    motion: ImageViewerMotion,
    state: VideoPlaybackStatus,
): VideoChromeActivity {
    val activity = remember(motion) { VideoChromeActivity() }
    val context = LocalContext.current
    val screenReader = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    val accessibility = LocalAccessibilityManager.current
    val timeout =
        accessibility?.calculateRecommendedTimeoutMillis(
            originalTimeoutMillis = VIDEO_CONTROLS_TIMEOUT_MS,
            containsIcons = true,
            containsText = true,
            containsControls = true,
        ) ?: VIDEO_CONTROLS_TIMEOUT_MS
    LaunchedEffect(motion, state.playing, state.stage) {
        if (!state.playing && state.stage == VideoStage.READY) motion.setControlsVisible(true)
    }
    val playingReady = state.playing && state.stage == VideoStage.READY
    val canHide = !activity.touching && screenReader?.isTouchExplorationEnabled != true
    LaunchedEffect(
        motion,
        motion.controlsVisible,
        playingReady,
        canHide,
        activity.revision,
        timeout,
    ) {
        if (motion.controlsVisible && playingReady && canHide) {
            delay(timeout)
            motion.setControlsVisible(false)
        }
    }
    return activity
}

internal fun Modifier.videoChromeActivity(activity: VideoChromeActivity): Modifier =
    pointerInput(activity) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                activity.onPointer(event.changes.any { it.pressed })
            }
        }
    }

/** Панель видео исчезает через 5 секунд без касаний. */
internal const val VIDEO_CONTROLS_TIMEOUT_MS = 5_000L
