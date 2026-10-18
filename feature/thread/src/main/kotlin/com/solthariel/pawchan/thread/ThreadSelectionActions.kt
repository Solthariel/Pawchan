package com.solthariel.pawchan.thread

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun rememberThreadSelectionActions(
    content: ThreadUiState.Content?,
    tools: ThreadToolsState,
    actions: ThreadActions,
    presentation: ThreadPresentation,
    exports: ThreadExportActions,
): ThreadSelectionActions {
    val scope = rememberCoroutineScope()
    val quote: () -> Unit = {
        val selected = tools.selected.toSet()
        scope.launch {
            val text = withContext(Dispatchers.Default) { selected.sorted().joinToString("\n") { ">>$it" } }
            actions.onReplyTo("$text\n")
            tools.clearSelection()
        }
    }
    val report: (() -> Unit)? =
        presentation.reports?.takeIf { it.available }?.let { controller ->
            {
                controller.reset()
                tools.reportTargets = null
                tools.dialog =
                    ThreadDialog.REPORT
            }
        }
    val showHidden: (ThreadPostUi) -> Unit = { post ->
        scope.launch {
            if (post.metadata.contentFilterLabel != null) {
                presentation.postContext?.revealContentFilter?.invoke(post.id)
                return@launch
            }
            val changed =
                presentation.postContext
                    ?.preferences
                    ?.change(PostPreferenceAction.SHOW, post) == true
            if (!changed) tools.notice = "Не удалось показать скрытый пост"
        }
    }
    return ThreadSelectionActions(quote, { if (content != null) exports.screenshot(content) }, report, showHidden)
}

internal data class ThreadSelectionActions(
    val quote: () -> Unit,
    val screenshot: () -> Unit,
    val report: (() -> Unit)?,
    val showHidden: (ThreadPostUi) -> Unit = {},
)
