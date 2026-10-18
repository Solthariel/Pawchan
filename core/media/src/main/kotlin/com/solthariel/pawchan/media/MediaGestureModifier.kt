package com.solthariel.pawchan.media

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
internal fun Modifier.mediaViewerGestures(
    motion: ImageViewerMotion,
    navigation: MediaViewerNavigation,
    onClose: () -> Unit,
    onLongPress: () -> Unit,
): Modifier =
    composed {
        val context = LocalContext.current
        val currentNavigation by rememberUpdatedState(navigation)
        val currentClose by rememberUpdatedState(onClose)
        val currentLongPress by rememberUpdatedState(onLongPress)
        val owner =
            remember(motion, context) {
                NativeMediaGestureOwner(
                    context,
                    motion,
                    { currentNavigation },
                    { currentClose() },
                    { currentLongPress() },
                )
            }
        DisposableEffect(owner) { onDispose { owner.dispose() } }
        pointerInteropFilter(onTouchEvent = owner::touch)
    }
