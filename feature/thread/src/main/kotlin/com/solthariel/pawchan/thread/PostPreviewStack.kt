package com.solthariel.pawchan.thread

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

/** Only three live window trees/models; older levels retain lightweight saveable UI state. */
@Composable
internal fun PostPreviewStack(
    targets: List<PostPreviewTarget>,
    local: ThreadUiState.Content,
    appearance: ThreadAppearance,
    resources: PostPreviewResources,
    bindings: @Composable (PostPreviewTarget) -> PostPreviewBindings,
    onClear: () -> Unit,
) {
    val path = remember { PreviewStackPath() }
    val entries = remember(targets) { path.reconcile(targets) }
    val holder = rememberSaveableStateHolder()
    val known = remember { mutableSetOf<String>() }
    LaunchedEffect(entries) {
        val current = entries.map { it.id }.toSet()
        (known - current).forEach(holder::removeState)
        known.clear()
        known.addAll(current)
    }
    val top = entries.lastOrNull() ?: return
    val motion = remember(top.id) { PreviewSwipeState() }
    Box(
        Modifier.fillMaxSize().drawBehind {
            drawRect(Color.Black.copy(alpha = PREVIEW_DIM))
        },
    ) {
        entries.takeLast(RETAINED_PREVIEW_WINDOWS).forEach { entry ->
            key(entry.id) {
                holder.SaveableStateProvider(entry.id) {
                    PreviewEntryOwner {
                        CompositionLocalProvider(
                            LocalPreviewLayer provides
                                PreviewLayer(
                                    depth = entries.lastIndex - entries.indexOf(entry),
                                    motion = motion,
                                    clear = onClear,
                                    count = entries.size.coerceAtMost(RETAINED_PREVIEW_WINDOWS),
                                ),
                        ) {
                            PostPreviewDialog(entry.target, local, appearance, bindings(entry.target), resources)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewEntryOwner(content: @Composable () -> Unit) {
    val owner =
        remember {
            object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
        }
    DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner, content = content)
}
