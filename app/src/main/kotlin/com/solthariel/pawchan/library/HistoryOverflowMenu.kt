package com.solthariel.pawchan.library

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalResources
import com.solthariel.pawchan.catalog.CatalogIconButton
import com.solthariel.pawchan.core.ui.ActionMenuItem
import com.solthariel.pawchan.core.ui.AppAlertDialog
import com.solthariel.pawchan.core.ui.ModalDropdownMenu
import com.solthariel.pawchan.core.ui.TransientNotice
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.domain.library.HistoryCleanup
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.workspace.LocalWorkspace
import kotlinx.coroutines.launch
import com.solthariel.pawchan.R.string as L

@Composable
internal fun HistoryOverflowMenu(onCleared: () -> Unit) {
    val res = LocalResources.current

    val cleanup = LocalWorkspace.current?.library as? HistoryCleanup
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    Box {
        CatalogIconButton(UiIconKind.MORE, res.getString(L.ui_history_menu), onClick = { open = true })
        ModalDropdownMenu(open, onDismissRequest = { open = false }) {
            ActionMenuItem(res.getString(L.ui_clear_history), {
                open = false
                confirm = true
            }, enabled = cleanup != null && !busy, icon = UiIconKind.DELETE)
        }
    }
    if (confirm) {
        AppAlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(res.getString(L.ui_clear_browsing_history)) },
            text = { Text(res.getString(L.ui_favorites_open_pages_drafts_and)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    busy = true
                    scope.launch {
                        message =
                            if (cleanup?.clearVisitHistory() is DomainOutcome.Success) {
                                onCleared()
                                res.getString(L.ui_history_cleared)
                            } else {
                                res.getString(L.ui_failed_to_clear_history)
                            }
                        busy = false
                    }
                }) { Text(res.getString(L.ui_clear)) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(res.getString(L.ui_cancel)) } },
        )
    }
    TransientNotice(message, onDismiss = { message = null })
}
