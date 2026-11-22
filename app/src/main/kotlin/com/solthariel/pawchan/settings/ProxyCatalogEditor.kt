package com.solthariel.pawchan.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.solthariel.pawchan.core.ui.FullScreenModalWindow
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton
import com.solthariel.pawchan.data.network.proxy.proxyRouteFor
import com.solthariel.pawchan.domain.proxy.ProxyMode
import com.solthariel.pawchan.domain.proxy.SavedProxy
import com.solthariel.pawchan.domain.proxy.validProxyCredentials
import java.util.UUID

internal fun newProxy() = SavedProxy(UUID.randomUUID().toString(), "", ProxyMode.SOCKS5, "", true, emptySet())

@Composable
internal fun ProxyCatalogEditor(
    initial: SavedProxy,
    catalog: List<SavedProxy>,
    mirror: String,
    onClose: () -> Unit,
    onSave: (SavedProxy) -> Unit,
    onDelete: () -> Unit,
) {
    val state = remember(initial.id) { ProxyEditorState(initial) }
    val current = state.current
    val conflict =
        catalog.any {
            it.id != current.id && it.enabled && current.enabled && it.services.any(current.services::contains)
        }
    val close = { if (state.dirty) state.discarding = true else onClose() }
    FullScreenModalWindow(
        close,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .systemBarsPadding()
                .imePadding(),
        ) {
            SettingsPageToolbar("Прокси", close) {
                if (catalog.any { it.id == initial.id }) {
                    UiToolbarButton(UiIconKind.DELETE, "Удалить", onClick = { state.deleting = true })
                }
                UiToolbarButton(UiIconKind.SAVE, "Сохранить", enabled = validDraft(current) && !conflict) {
                    onSave(current.copy(name = current.name.trim()))
                }
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProxyEditorFields(state)
                if (conflict) {
                    Text("Ресурс уже назначен другому включённому прокси", color = MaterialTheme.colorScheme.error)
                }
                ProxyDraftChecks(current, mirror)
            }
        }
    }
    ProxyEditorDialogs(state, onClose, onDelete)
}

@Composable
private fun ProxyEditorDialogs(
    state: ProxyEditorState,
    onClose: () -> Unit,
    onDelete: () -> Unit,
) {
    if (state.deleting) {
        ConfirmationDialog(
            "Удалить прокси?",
            "Назначения этого профиля будут удалены.",
            "Удалить",
            { state.deleting = false },
        ) {
            state.deleting = false
            onDelete()
        }
    }
    if (state.discarding) {
        ConfirmationDialog(
            "Не сохранять изменения?",
            "Изменения будут потеряны.",
            "Не сохранять",
            { state.discarding = false },
            onClose,
        )
    }
}

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    action: String,
    close: () -> Unit,
    confirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = close,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = confirm) { Text(action) } },
        dismissButton = { TextButton(onClick = close) { Text("Отмена") } },
    )
}

internal fun validDraft(draft: SavedProxy): Boolean =
    draft.name.isNotBlank() && draft.name.length <= MAX_PROXY_NAME_LENGTH && draft.services.isNotEmpty() &&
        proxyRouteFor(draft.mode, draft.address, draft.credentials) != null &&
        (draft.credentials?.let { validProxyCredentials(it, draft.mode) } ?: true)

private const val MAX_PROXY_NAME_LENGTH = 100
