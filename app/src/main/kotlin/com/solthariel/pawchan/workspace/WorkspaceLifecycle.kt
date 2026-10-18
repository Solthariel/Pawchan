package com.solthariel.pawchan.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
internal fun BindWorkspaceLifecycle(controller: WorkspaceController) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, controller) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event ==
                    Lifecycle.Event.ON_RESUME
                ) {
                    controller.reload()
                }
            }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
}
