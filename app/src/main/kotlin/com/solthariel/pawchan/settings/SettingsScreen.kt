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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.BuildConfig
import com.solthariel.pawchan.R
import com.solthariel.pawchan.catalog.CatalogBottomActions
import com.solthariel.pawchan.catalog.CatalogBottomBar
import com.solthariel.pawchan.catalog.CatalogBottomDestination
import com.solthariel.pawchan.catalog.rememberCatalogSearchState
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.update.UpdateConfiguration
import com.solthariel.pawchan.update.UpdatePanel

/** Settings search lives in the fixed toolbar, just as in the catalog. */
@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    bottomActions: CatalogBottomActions,
) {
    var section by rememberSaveable { mutableStateOf<SettingsSection?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val content = settingsContent(state, actions, rememberSettingsEnvironment { ReleaseUpdatePanel() })
    BackHandler(enabled = section != null) { section = null }
    val search = rememberCatalogSearchState(query) { query = it }
    val openSection: (SettingsSection?) -> Unit = { next ->
        search.close()
        section = next
    }
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        com.solthariel.pawchan.core.ui.UiPageTransition(
            section,
            depth = { if (it == null) 0 else 1 },
            modifier = Modifier.weight(1f),
        ) { activeSection ->
            Column(Modifier.fillMaxSize().padding(bottom = SETTINGS_BOTTOM_PADDING)) {
                if (activeSection == SettingsSection.AUTOHIDE) {
                    ContentFilterSettings(state, actions) { section = null }
                } else {
                    SettingsToolbar(
                        activeSection?.title ?: stringResource(R.string.settings_title),
                        search,
                        "Поиск настроек",
                        { if (section != null) openSection(null) else actions.onBack() },
                    ) {
                        SettingsOverflowMenu(openSection)
                    }
                    key(activeSection, query.isNotBlank()) {
                        SettingsBody(activeSection, query, content, openSection, Modifier.weight(1f))
                    }
                }
            }
        }
        // Settings sections are local UI state, not NavController destinations.
        CatalogBottomBar(
            actions = bottomActions.copy(onSettingsClick = { openSection(null) }),
            selected = CatalogBottomDestination.SETTINGS,
        )
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
    val scroll = rememberScrollState()
    Column(modifier.fillMaxWidth().verticalScroll(scroll)) {
        if (query.isNotBlank()) {
            val found = searchSettings(query, content)
            if (found.isEmpty()) {
                Text("Ничего не найдено", Modifier.padding(24.dp), style = MaterialTheme.typography.bodyLarge)
            } else {
                found.forEach { SettingsGroupCard(it) }
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
                    Text(section.title, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        section.summary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReleaseUpdatePanel() {
    UpdatePanel(
        configuration =
            UpdateConfiguration(
                manifestUrl = BuildConfig.UPDATE_MANIFEST_URL,
                currentVersionCode = BuildConfig.VERSION_CODE.toLong(),
                currentVersionName = BuildConfig.VERSION_NAME,
            ),
    )
}

internal val ThemePreference.labelResource: Int
    get() =
        when (this) {
            ThemePreference.SYSTEM -> R.string.theme_system
            ThemePreference.LIGHT -> R.string.theme_light
            ThemePreference.DARK -> R.string.theme_dark
            ThemePreference.AMOLED -> R.string.theme_amoled
            ThemePreference.TOMORROW -> R.string.theme_tomorrow
            ThemePreference.NORMIE -> R.string.theme_normie
            ThemePreference.BURICHAN -> R.string.theme_burichan
            ThemePreference.HARDON -> R.string.theme_hardon
            ThemePreference.NEUTRON -> R.string.theme_neutron
            ThemePreference.PHOTON -> R.string.theme_photon
        }

private val SETTINGS_BOTTOM_PADDING = 8.dp
