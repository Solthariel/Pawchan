package com.solthariel.pawchan.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.catalog.rememberCatalogSearchState
import com.solthariel.pawchan.core.ui.AppAlertDialog
import com.solthariel.pawchan.core.ui.FloatingTopChrome
import com.solthariel.pawchan.core.ui.LocalBottomOverlayInset
import com.solthariel.pawchan.core.ui.LocalTopOverlayInset
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton
import com.solthariel.pawchan.infrastructure.CookieRuleStore
import com.solthariel.pawchan.infrastructure.CookieRules
import com.solthariel.pawchan.infrastructure.SiteCookies
import com.solthariel.pawchan.infrastructure.WebViewSiteCookies
import kotlinx.coroutines.flow.StateFlow
import com.solthariel.pawchan.R.string as L

/** «Сеть → Куки»: куки 2ch на текущем зеркале, как «Управление cookie» в Dashchan. */
@Composable
internal fun CookiesSettings(
    mirror: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val controller = remember(mirror) { CookiesController(WebViewSiteCookies(mirror), CookieRuleStore.get(context)) }
    CookiesPage(controller, onBack)
}

@Composable
internal fun CookiesPage(
    controller: CookiesController,
    onBack: () -> Unit,
) {
    val res = LocalResources.current
    val rules by controller.rules.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    val search = rememberCatalogSearchState(query) { query = it }
    BackHandler(onBack = onBack)
    FloatingTopChrome(toolbar = {
        SettingsToolbar(res.getString(L.ui_cookies), search, res.getString(L.ui_search), onBack) {
            UiToolbarButton(
                UiIconKind.CLEANING,
                res.getString(L.ui_cookies_clear_all),
                enabled = controller.cookies.isNotEmpty(),
            ) { confirmClear = true }
        }
    }) {
        val term = query.trim()
        val entries = cookieEntries(controller.cookies, rules).filter { it.name.contains(term, ignoreCase = true) }
        CookiesBody(controller, entries)
    }
    if (confirmClear) ClearCookiesDialog(controller.host, { confirmClear = false }, controller::clearAll)
}

@Composable
private fun CookiesBody(
    controller: CookiesController,
    entries: List<CookieEntry>,
) {
    val res = LocalResources.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(
            top = LocalTopOverlayInset.current,
            bottom = LocalBottomOverlayInset.current + 8.dp,
        ),
    ) {
        SettingsGroupHeading("2ch · ${controller.host}")
        SettingsGroupFrame {
            if (entries.isEmpty()) Text(res.getString(L.ui_cookies_empty), Modifier.padding(16.dp))
            entries.forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                CookieRow(entry, controller)
            }
        }
        Text(
            res.getString(L.ui_cookies_only_dvach),
            Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ClearCookiesDialog(
    host: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val res = LocalResources.current
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(res.getString(L.ui_cookies_clear_all_confirm, host)) },
        text = { Text(res.getString(L.ui_cookies_clear_all_hint)) },
        dismissButton = { TextButton(onClick = onDismiss) { Text(res.getString(L.ui_cancel)) } },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) { Text(res.getString(L.ui_cookies_clear_all), color = MaterialTheme.colorScheme.error) }
        },
    )
}

/** Список сайта и правила; каждое изменение сразу пишется в WebView и в правила. */
internal class CookiesController(
    private val site: SiteCookies,
    private val store: CookieRuleStore,
) {
    var cookies by mutableStateOf(site.list())
        private set
    val host: String get() = site.host
    val rules: StateFlow<CookieRules> get() = store.rules

    /** Блокировка сразу удаляет куку: дальше её не принимаем и не отправляем. */
    fun block(
        name: String,
        on: Boolean,
    ) {
        store.update { it.withBlocked(name, on) }
        if (on) delete(name)
    }

    fun deleteOnExit(
        name: String,
        on: Boolean,
    ) = store.update { it.withDeleteOnExit(name, on) }

    fun delete(name: String) {
        site.delete(name)
        cookies = site.list()
    }

    fun clearAll() {
        cookies.forEach { site.delete(it.first) }
        cookies = site.list()
    }
}
