package com.solthariel.pawchan.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.R
import com.solthariel.pawchan.catalog.CatalogSearchState
import com.solthariel.pawchan.catalog.rememberCatalogSearchState
import com.solthariel.pawchan.core.ui.LocalBottomOverlayInset
import com.solthariel.pawchan.core.ui.UiForwardIndicator
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.data.network.proxy.proxyRouter
import com.solthariel.pawchan.data.network.proxy.viaProxyRouter
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.domain.settings.displayMode
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.navigation.chrome.AppBottomDestination
import com.solthariel.pawchan.navigation.chrome.AppNavigationWorkspace
import com.solthariel.pawchan.update.rememberUpdatePanel
import com.solthariel.pawchan.R.string as L

/** Settings search lives in the fixed toolbar, just as in the catalog. */
@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    bottomActions: AppBottomActions,
) {
    var section by rememberSaveable { mutableStateOf<SettingsSection?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val search = rememberCatalogSearchState(query) { query = it }
    val history = rememberChangelogHistory()
    val aboutEntry = remember(section) { if (section == SettingsSection.ABOUT) Any() else null }
    LaunchedEffect(aboutEntry) { aboutEntry?.let(history::enter) }
    val githubNetwork = com.solthariel.pawchan.infrastructure.LocalGithubNetwork.current
    val router = githubNetwork.historyClient.proxyRouter()
    val configure =
        remember(router) {
            { builder: okhttp3.OkHttpClient.Builder ->
                router?.let { builder.viaProxyRouter(it) }
                Unit
            }
        }
    val updates =
        rememberUpdatePanel(
            releaseUpdateConfiguration(),
            onUpdateAvailable = history::updateFound,
            configure = configure,
        )
    val environment =
        rememberSettingsEnvironment { AboutUpdateRow(updates) }
            .copy(
                changelog = { ChangelogSettings(history, showVersion = true) },
                openProxy = {
                    search.close()
                    section = SettingsSection.NETWORK
                },
            )
    val content = settingsContent(state, actions, environment)
    BackHandler(enabled = section != null) { section = null }
    val openSection: (SettingsSection?) -> Unit = { next ->
        search.close()
        section = next
    }
    AppNavigationWorkspace(
        actions = bottomActions.copy(onSettingsClick = { openSection(null) }),
        selected = AppBottomDestination.SETTINGS,
        floatingTop = true,
    ) {
        com.solthariel.pawchan.core.ui.UiPageTransition(
            section,
            depth = { if (it == null) 0 else 1 },
            modifier = Modifier.fillMaxSize(),
        ) { activeSection ->
            SettingsSectionSurface(activeSection, state, actions, search, content, openSection)
        }
    }
}

@Composable
private fun SettingsSectionSurface(
    active: SettingsSection?,
    state: SettingsUiState,
    actions: SettingsActions,
    search: CatalogSearchState,
    content: Map<SettingsSection, List<SettingsGroup>>,
    openSection: (SettingsSection?) -> Unit,
) {
    val res = LocalResources.current

    when (active) {
        SettingsSection.AUTOHIDE -> {
            ContentFilterSettings(state, actions) { openSection(null) }
        }

        SettingsSection.NETWORK -> {
            ProxyCatalogSettings(state.app.behavior.client.mirror) { openSection(null) }
        }

        else -> {
            com.solthariel.pawchan.core.ui.FloatingTopChrome(toolbar = {
                SettingsToolbar(
                    active?.title?.let {
                        androidx.compose.ui.platform.LocalResources.current
                            .settingsLabel(it)
                    }
                        ?: stringResource(R.string.settings_title),
                    search,
                    res.getString(L.ui_search_settings),
                    { if (active != null) openSection(null) else actions.onBack() },
                ) { SettingsOverflowMenu(openSection) }
            }) {
                key(active, search.query.isNotBlank()) {
                    SettingsBody(active, search.query, content, openSection, Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun SettingsBody(
    section: SettingsSection?,
    query: String,
    content: Map<SettingsSection, List<SettingsGroup>>,
    onOpen: (SettingsSection) -> Unit,
    modifier: Modifier,
) {
    val res = LocalResources.current

    val scroll = rememberScrollState()
    Column(
        modifier
            .fillMaxWidth()
            .testTag("settings-body-scroll")
            .verticalScroll(scroll)
            .padding(
                top = com.solthariel.pawchan.core.ui.LocalTopOverlayInset.current,
                bottom = LocalBottomOverlayInset.current + 8.dp,
            ),
    ) {
        if (query.isNotBlank()) {
            val found = searchSettings(query, content)
            if (found.isEmpty()) {
                Text(
                    res.getString(L.ui_nothing_found),
                    Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                found.forEach {
                    SettingsGroupCard(
                        it.copy(
                            title =
                                androidx.compose.ui.platform.LocalResources.current
                                    .settingsLabel(it.title),
                        ),
                    )
                }
            }
        } else if (section != null) {
            content[section].orEmpty().forEach { SettingsGroupCard(it) }
        } else {
            SettingsSectionList(onOpen)
        }
    }
}

@Composable
private fun SettingsSectionList(onOpen: (SettingsSection) -> Unit) {
    SettingsGroupFrame(Modifier.padding(vertical = 12.dp)) {
        SettingsSection.entries.forEachIndexed { index, section ->
            if (index > 0) HorizontalDivider(Modifier.padding(start = 56.dp, end = 16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(section) }
                    .heightIn(min = SETTINGS_DOUBLE_ROW_HEIGHT)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UiIcon(section.icon, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(24.dp))
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(
                        androidx.compose.ui.platform.LocalResources.current
                            .settingsLabel(section.title),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        androidx.compose.ui.platform.LocalResources.current
                            .settingsLabel(section.summary),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                UiForwardIndicator()
            }
        }
    }
}

internal val ThemePreference.labelResource: Int
    get() =
        when {
            this == ThemePreference.AMOLED -> R.string.theme_amoled
            displayMode() == ThemePreference.SYSTEM -> R.string.theme_system
            displayMode() == ThemePreference.LIGHT -> R.string.theme_light
            else -> R.string.theme_dark
        }
