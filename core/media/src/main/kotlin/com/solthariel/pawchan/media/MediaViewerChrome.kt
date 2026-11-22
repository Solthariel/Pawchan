package com.solthariel.pawchan.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset

@Composable
internal fun MediaBackdrop(
    motion: ImageViewerMotion,
    opacity: Float = 1f,
) {
    Box(Modifier.fillMaxSize().drawBehind { drawRect(Color.Black, alpha = motion.backgroundAlpha * opacity) })
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun MediaViewerChrome(
    motion: ImageViewerMotion,
    bottom: (@Composable () -> Unit)? = null,
    blackChrome: Boolean = false,
    content: @Composable () -> Unit,
) {
    MediaSystemBars(motion.controlsVisible)
    val chromeColor = if (blackChrome) Color.Black.copy(alpha = VIDEO_CHROME_ALPHA) else MEDIA_CHROME_BACKGROUND
    MaterialTheme(colorScheme = MEDIA_CHROME_COLORS) {
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = motion.controlsVisible,
                enter =
                    fadeIn(tween(BAR_ANIMATION_MS)) +
                        slideInVertically(
                            tween(
                                BAR_ANIMATION_MS,
                                easing = ReferenceMediaMotion.restoreEasing,
                            ),
                        ) { -it },
                exit = fadeOut(tween(BAR_ANIMATION_MS)) + slideOutVertically(tween(BAR_ANIMATION_MS)) { -it },
            ) {
                // One layer covers status inset and toolbar; the toolbar itself is transparent.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(chromeColor)
                        .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility),
                ) { content() }
            }
            AnimatedVisibility(
                visible = motion.controlsVisible,
                modifier = Modifier.fillMaxSize(),
                enter = fadeIn(tween(BAR_ANIMATION_MS)),
                exit = fadeOut(tween(BAR_ANIMATION_MS)),
            ) {
                MediaNavigationScrim(chromeColor)
            }
            if (bottom != null) {
                AnimatedVisibility(
                    visible = motion.controlsVisible,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = fadeIn(tween(BAR_ANIMATION_MS)) + slideInVertically(barRestoreSpec()) { it },
                    exit = fadeOut(tween(BAR_ANIMATION_MS)) + slideOutVertically(tween(BAR_ANIMATION_MS)) { it },
                ) {
                    // Как у верхней панели: фон тот же, нижняя системная зона закрашена scrim-ом.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)
                            .background(chromeColor)
                            // Пустое место панели не должно скрывать интерфейс тапом «сквозь» неё.
                            .pointerInput(Unit) { detectTapGestures { } },
                    ) { bottom() }
                }
            }
        }
    }
}

private const val VIDEO_CHROME_ALPHA = 0.60f
private const val BAR_ANIMATION_MS = 250

private fun barRestoreSpec() = tween<IntOffset>(BAR_ANIMATION_MS, easing = ReferenceMediaMotion.restoreEasing)
