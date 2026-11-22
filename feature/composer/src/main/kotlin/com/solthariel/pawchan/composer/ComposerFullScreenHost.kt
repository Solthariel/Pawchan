package com.solthariel.pawchan.composer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import com.solthariel.pawchan.core.ui.KeepWindowBounds
import com.solthariel.pawchan.core.ui.FullScreenModalWindow as Dialog

@Composable
fun ComposerFullScreenHost(
    visible: Boolean,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
    form: @Composable () -> Unit,
) {
    val transition = remember { MutableTransitionState(false) }
    SideEffect { transition.targetState = visible }
    content()
    if (visible || transition.currentState || !transition.isIdle) {
        Dialog(
            onDismissRequest = onClose,
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            KeepWindowBounds()
            AnimatedVisibility(
                visibleState = transition,
                enter = fadeIn(tween(COMPOSER_ENTER_MS)),
                exit = fadeOut(tween(COMPOSER_EXIT_MS)),
            ) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { form() }
            }
        }
    }
}

private const val COMPOSER_ENTER_MS = 250
private const val COMPOSER_EXIT_MS = 200

/** The snapshot belongs to the animated content, so it is released after exit. */
@Composable
fun <T> StatefulComposerFullScreenHost(
    visible: Boolean,
    state: T,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
    form: @Composable (T) -> Unit,
) {
    ComposerFullScreenHost(visible, onClose, content) {
        form(rememberComposerTransitionSnapshot(visible, state))
    }
}
