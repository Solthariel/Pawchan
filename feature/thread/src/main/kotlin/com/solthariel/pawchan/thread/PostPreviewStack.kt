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
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.solthariel.pawchan.core.ui.ModalInlineLayer
import com.solthariel.pawchan.core.ui.modalInlineBackdrop

/** Only five live window trees/models; older levels retain lightweight saveable UI state. */
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
    val shared = remember(entries) { entries.map { SharedPreviewEntry(it.id, it.target) } }
    SharedPostPreviewStack(shared, onClear) { target ->
        PostPreviewDialog(target, local, appearance, bindings(target), resources)
    }
}

/** Uses the same stacked window behavior for any source with locally available posts. */
data class SharedPreviewEntry<T>(
    val id: String,
    val target: T,
)

@Composable
fun <T> SharedPostPreviewStack(
    entries: List<SharedPreviewEntry<T>>,
    onClear: () -> Unit,
    content: @Composable (T) -> Unit,
) {
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
    ModalInlineLayer {
        Box(Modifier.fillMaxSize().modalInlineBackdrop()) {
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
                                content(entry.target)
                            }
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
