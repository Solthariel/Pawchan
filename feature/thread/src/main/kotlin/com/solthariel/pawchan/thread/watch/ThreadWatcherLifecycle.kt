package com.solthariel.pawchan.thread.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
internal fun BindThreadWatcherLifecycle(controller: ThreadWatcherController) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, controller) {
        fun updateActiveState() {
            controller.setActive(
                owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED),
            )
        }
        val observer = LifecycleEventObserver { _, _ -> updateActiveState() }
        owner.lifecycle.addObserver(observer)
        updateActiveState()
        onDispose {
            owner.lifecycle.removeObserver(observer)
            controller.setActive(false)
        }
    }
}
