package com.solthariel.pawchan.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
internal fun BindWorkspaceReplyChecks(binding: WorkspaceBinding) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val model: ReplyMonitorViewModel = viewModel(factory = ReplyMonitorViewModel.factory(context, binding))
    DisposableEffect(lifecycle, model) {
        val observer =
            LifecycleEventObserver {
                _,
                _,
                ->
                model.setActive(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
            }
        lifecycle.addObserver(observer)
        model.setActive(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        onDispose {
            lifecycle.removeObserver(observer)
            model.setActive(false)
        }
    }
}
