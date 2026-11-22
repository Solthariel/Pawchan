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

@Composable
internal fun ProxyCatalogSettings(
    mirror: String,
    onBack: () -> Unit,
) {
    val runtime = LocalProxyCatalog.current
    if (runtime == null) {
        Text("Настройки прокси недоступны")
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
        )
    FloatingTopChrome(toolbar = {
        SettingsToolbar("Прокси", search, "Поиск прокси", onBack) {
            UiToolbarButton(UiIconKind.ADD, "Добавить прокси", enabled = !state.needsReset && !state.saving) {
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
)

@Composable
private fun ProxyCatalogBody(
    state: ProxyCatalogState,
    query: String,
    error: Boolean,
    actions: ProxyListActions,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(
            top = LocalTopOverlayInset.current,
            bottom = LocalBottomOverlayInset.current + 8.dp,
        ),
    ) {
        if (state.needsReset) {
            Text(
                "Старые или повреждённые настройки не поддерживаются; соединения заблокированы",
                Modifier.padding(16.dp),
            )
            TextButton(onClick = actions.reset) { Text("Сбросить настройки прокси") }
        }
        if (state.saveFailed) {
            Text("Не удалось сохранить; затронутые соединения заблокированы", Modifier.padding(16.dp))
            TextButton(onClick = actions.retry) { Text("Повторить сохранение") }
        }
        if (error) Text("Ресурс уже назначен другому включённому прокси", Modifier.padding(16.dp))
        val visible = state.catalog.filter { it.name.contains(query.trim(), ignoreCase = true) }
        SettingsGroupFrame {
            if (visible.isEmpty()) Text("Прокси не найдены", Modifier.padding(16.dp))
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
    AlertDialog(
        onDismissRequest = close,
        title = { Text("Сбросить настройки прокси?") },
        text = { Text("Все ресурсы будут использовать соединение без прокси приложения.") },
        confirmButton = {
            TextButton(onClick = {
                close()
                reset()
            }) { Text("Сбросить") }
        },
        dismissButton = { TextButton(onClick = close) { Text("Отмена") } },
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
                proxy.services.joinToString { it.proxyLabel }.ifEmpty { "Ресурсы не выбраны" },
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
