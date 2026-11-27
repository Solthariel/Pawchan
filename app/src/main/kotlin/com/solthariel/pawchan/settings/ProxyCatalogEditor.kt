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
import androidx.compose.ui.platform.LocalResources
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
import com.solthariel.pawchan.R.string as L

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
    val res = LocalResources.current

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
            SettingsPageToolbar(res.getString(L.ui_proxy), close) {
                if (catalog.any { it.id == initial.id }) {
                    UiToolbarButton(
                        UiIconKind.DELETE,
                        res.getString(L.ui_delete),
                        onClick = { state.deleting = true },
                    )
                }
                UiToolbarButton(
                    UiIconKind.SAVE,
                    res.getString(L.ui_save),
                    enabled =
                        validDraft(current) && !conflict,
                ) {
                    onSave(current.copy(name = current.name.trim()))
                }
            }
            Column(
                Modifier
                    .weight(
                        1f,
                    ).verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProxyEditorFields(state)
                if (conflict) {
                    Text(
                        res.getString(L.ui_the_resource_is_already_assigned),
                        color = MaterialTheme.colorScheme.error,
                    )
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
    val res = LocalResources.current

    if (state.deleting) {
        ConfirmationDialog(
            res.getString(L.ui_delete_proxy),
            res.getString(L.ui_assignments_for_this_profile_will),
            res.getString(L.ui_delete),
            { state.deleting = false },
        ) {
            state.deleting = false
            onDelete()
        }
    }
    if (state.discarding) {
        ConfirmationDialog(
            res.getString(L.ui_don_t_save_changes),
            res.getString(L.ui_changes_will_be_lost),
            res.getString(L.ui_do_not_save),
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
    val res = LocalResources.current

    AlertDialog(
        onDismissRequest = close,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = confirm) { Text(action) } },
        dismissButton = { TextButton(onClick = close) { Text(res.getString(L.ui_cancel)) } },
    )
}

internal fun validDraft(draft: SavedProxy): Boolean =
    draft.name.isNotBlank() && draft.name.length <= MAX_PROXY_NAME_LENGTH && draft.services.isNotEmpty() &&
        proxyRouteFor(draft.mode, draft.address, draft.credentials) != null &&
        (draft.credentials?.let { validProxyCredentials(it, draft.mode) } ?: true)

private const val MAX_PROXY_NAME_LENGTH = 100
