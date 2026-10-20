package com.solthariel.pawchan.thread

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.solthariel.pawchan.feature.thread.R

@Composable
internal fun BindThreadScreenEffects(
    memory: ThreadScreenMemory,
    content: ThreadUiState.Content?,
    actions: ThreadActions,
    exports: ThreadExportActions,
) {
    val tools = memory.tools
    val context = LocalContext.current
    LaunchedEffect(content?.address, content?.presentation?.missingThreadNotice) {
        if ((content?.presentation?.missingThreadNotice ?: 0) > 0) {
            Toast.makeText(context, R.string.thread_not_exists, Toast.LENGTH_SHORT).show()
        }
    }
    DisposableEffect(tools) { onDispose { tools.cancel() } }
    LaunchedEffect(content, tools.flag, tools.searchOpen) {
        if (tools.searchOpen) tools.search(content)
    }
    LaunchedEffect(tools.results) { tools.results.firstOrNull()?.let { memory.goTo(content, it) } }
    LaunchedEffect(tools.notice) {
        when (val notice = tools.notice) {
            ARCHIVE_REQUEST -> {
                tools.notice = null
                if (content != null) exports.archive(content)
            }

            null -> {
                Unit
            }

            else -> {
                Toast.makeText(context, notice, Toast.LENGTH_SHORT).show()
                tools.notice = null
            }
        }
    }
    BackHandler {
        val target =
            threadBackTarget(
                memory.opened != null,
                tools.dialog != ThreadDialog.NONE,
                tools.selecting,
                tools.searchOpen,
            )
        when (target) {
            ThreadBackTarget.MEDIA -> memory.opened = null
            ThreadBackTarget.DIALOG -> tools.dialog = ThreadDialog.NONE
            ThreadBackTarget.SELECTION -> tools.clearSelection()
            ThreadBackTarget.SEARCH -> tools.closeSearch()
            ThreadBackTarget.ROUTE -> actions.onBack()
        }
    }
}
