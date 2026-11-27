package com.solthariel.pawchan.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.catalog.rememberCatalogSearchState
import com.solthariel.pawchan.core.ui.FloatingTopChrome
import com.solthariel.pawchan.core.ui.LocalBottomOverlayInset
import com.solthariel.pawchan.core.ui.LocalTopOverlayInset
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton
import com.solthariel.pawchan.data.network.proxy.ProxyCatalogRuntime
import com.solthariel.pawchan.data.network.proxy.ProxyCatalogState
import com.solthariel.pawchan.domain.proxy.ProxyService
import com.solthariel.pawchan.domain.proxy.SavedProxy
import com.solthariel.pawchan.infrastructure.LocalProxyCatalog
import com.solthariel.pawchan.R.string as L

@Composable
internal fun ProxyCatalogSettings(
    mirror: String,
    onBack: () -> Unit,
) {
    val res = LocalResources.current
    var cookiesOpen by rememberSaveable { mutableStateOf(false) }
    if (cookiesOpen) {
        CookiesSettings(mirror) { cookiesOpen = false }
        return
    }
    val runtime = LocalProxyCatalog.current
    if (runtime == null) {
        Text(res.getString(L.ui_proxy_settings_unavailable))
        return
    }
    val state by runtime.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<SavedProxy?>(null) }
    var error by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val search = rememberCatalogSearchState(query) { query = it }
    val actions =
        ProxyListActions(
            edit = { editing = it },
            reset = { confirmReset = true },
            retry = { if (runtime.save(state.catalog)) error = false },
            toggle = { proxy, enabled ->
                val next = state.catalog.map { if (it.id == proxy.id) it.copy(enabled = enabled) else it }
                error = !runtime.save(next)
            },
            cookies = { cookiesOpen = true },
        )
    FloatingTopChrome(toolbar = {
        SettingsToolbar(
            res.getString(L.ui_proxy),
            search,
            res.getString(L.ui_search_proxies),
            onBack,
        ) {
            UiToolbarButton(
                UiIconKind.ADD,
                res.getString(L.ui_add_proxy),
                enabled = !state.needsReset && !state.saving,
            ) {
                editing = newProxy()
            }
            ProxyCatalogMenu()
        }
    }) { ProxyCatalogBody(state, query, error, actions) }
    if (confirmReset) ProxyResetDialog({ confirmReset = false }) { runtime.save(emptyList()) }
    editing?.let { proxy ->
        ProxyEditorHost(proxy, state.catalog, mirror, runtime, {
            editing = null
            error = false
        }) { error = true }
    }
}

private data class ProxyListActions(
    val edit: (SavedProxy) -> Unit,
    val reset: () -> Unit,
    val retry: () -> Unit,
    val toggle: (SavedProxy, Boolean) -> Unit,
    val cookies: () -> Unit,
)

@Composable
private fun ProxyCatalogBody(
    state: ProxyCatalogState,
    query: String,
    error: Boolean,
    actions: ProxyListActions,
) {
    val res = LocalResources.current

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(
            top = LocalTopOverlayInset.current,
            bottom = LocalBottomOverlayInset.current + 8.dp,
        ),
    ) {
        SettingsGroupFrame(Modifier.padding(bottom = 8.dp)) {
            SettingsRow(res.getString(L.ui_cookies), res.getString(L.ui_cookies_summary), onClick = actions.cookies) {
                com.solthariel.pawchan.core.ui
                    .UiForwardIndicator()
            }
        }
        if (state.needsReset) {
            Text(
                res.getString(L.ui_old_or_corrupted_settings_are),
                Modifier.padding(16.dp),
            )
            TextButton(onClick = actions.reset) { Text(res.getString(L.ui_reset_proxy_settings)) }
        }
        if (state.saveFailed) {
            Text(res.getString(L.ui_failed_to_save_affected_connections), Modifier.padding(16.dp))
            TextButton(onClick = actions.retry) { Text(res.getString(L.ui_retry_saving)) }
        }
        if (error) Text(res.getString(L.ui_the_resource_is_already_assigned), Modifier.padding(16.dp))
        val visible = state.catalog.filter { it.name.contains(query.trim(), ignoreCase = true) }
        SettingsGroupFrame {
            if (visible.isEmpty()) Text(res.getString(L.ui_no_proxies_found), Modifier.padding(16.dp))
            visible.forEachIndexed { index, proxy ->
                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                ProxyCatalogRow(proxy, !state.saving, { actions.edit(proxy) }) { actions.toggle(proxy, it) }
            }
        }
    }
}

@Composable
private fun ProxyResetDialog(
    close: () -> Unit,
    reset: () -> Unit,
) {
    val res = LocalResources.current

    AlertDialog(
        onDismissRequest = close,
        title = { Text(res.getString(L.ui_reset_proxy_settings_c4cc)) },
        text = { Text(res.getString(L.ui_all_resources_will_use_a)) },
        confirmButton = {
            TextButton(onClick = {
                close()
                reset()
            }) { Text(res.getString(L.ui_reset)) }
        },
        dismissButton = { TextButton(onClick = close) { Text(res.getString(L.ui_cancel)) } },
    )
}

@Composable
private fun ProxyEditorHost(
    proxy: SavedProxy,
    catalog: List<SavedProxy>,
    mirror: String,
    runtime: ProxyCatalogRuntime,
    close: () -> Unit,
    onError: () -> Unit,
) {
    ProxyCatalogEditor(proxy, catalog, mirror, onClose = close, onSave = { saved ->
        val next =
            if (catalog.any { it.id == saved.id }) {
                catalog.map { if (it.id == saved.id) saved else it }
            } else {
                catalog + saved
            }
        if (runtime.save(next)) close() else onError()
    }, onDelete = {
        if (runtime.save(catalog.filterNot { it.id == proxy.id })) close()
    })
}

@Composable
private fun ProxyCatalogRow(
    proxy: SavedProxy,
    enabled: Boolean,
    edit: () -> Unit,
    change: (Boolean) -> Unit,
) {
    val res = LocalResources.current

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = edit)
            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(proxy.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                proxy.services.joinToString { it.proxyLabel }.ifEmpty {
                    res.getString(
                        L.ui_no_resources_selected,
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Checkbox(checked = proxy.enabled, enabled = enabled, onCheckedChange = change)
    }
}

internal val ProxyService.proxyLabel: String get() =
    when (this) {
        ProxyService.DVACH -> "2ch"
        ProxyService.FOUR_CHAN -> "4chan"
        ProxyService.ARCHIVE -> "Архивач"
        ProxyService.VOCAROO -> "Vocaroo"
        ProxyService.GITHUB -> "GitHub"
    }
