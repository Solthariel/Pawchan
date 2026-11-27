package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.catalog.rememberCatalogSearchState
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.filter.ContentFilterStatistics
import com.solthariel.pawchan.navigation.defaultBoardAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import com.solthariel.pawchan.R.string as L

@Composable
internal fun ContentFilterSettings(
    state: SettingsUiState,
    actions: SettingsActions,
    onBack: (() -> Unit)? = null,
) {
    val res = LocalResources.current

    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var helpVisible by rememberSaveable { mutableStateOf(false) }
    var statisticsVisible by rememberSaveable { mutableStateOf(false) }
    val rules = state.app.contentFilters
    val visible by visibleContentFilters(rules, query)
    val search = rememberCatalogSearchState(query) { query = it }
    val backup = rememberContentFilterBackup(rules, actions.onRestoreFilters)
    val statistics by (actions.filterStatistics?.statistics ?: flowOf(ContentFilterStatistics(available = false)))
        .collectAsStateWithLifecycle(ContentFilterStatistics(available = false))
    val menu =
        ContentFilterMenuActions({ editing = "new" }, backup, { statisticsVisible = true }, { helpVisible = true })
    com.solthariel.pawchan.core.ui.FloatingTopChrome(toolbar = {
        ContentFilterToolbar(onBack, search, menu)
        if (state.app.behavior.contentFilterResetNotice) {
            ContentFilterResetNotice {
                actions.onUpdate { app ->
                    app.copy(
                        behavior = app.behavior.copy(autohide = app.behavior.autohide.copy(resetNotice = false)),
                    )
                }
            }
        }
    }) {
        ContentFilterRulesList(
            visible,
            statistics,
            if (rules.isEmpty()) res.getString(L.ui_no_rules_yet) else res.getString(L.ui_no_matching_rules),
            Modifier.fillMaxSize(),
            setEnabled = { id, enabled ->
                actions.onUpdate { app -> app.withContentFilters(app.contentFilters.withRuleEnabled(id, enabled)) }
            },
        ) { editing = it }
    }
    if (helpVisible) AutohideHelpDialog { helpVisible = false }
    if (statisticsVisible) {
        ContentFilterStatisticsDialog(statistics, actions.filterStatistics) { statisticsVisible = false }
    }
    editing?.let { id ->
        ContentFilterSettingsEditor(state, actions, id) { editing = null }
    }
}

@Composable
private fun visibleContentFilters(
    rules: List<ContentFilterRule>,
    query: String,
): State<List<ContentFilterRule>> =
    produceState(emptyList<ContentFilterRule>(), rules, query) {
        value = withContext(Dispatchers.Default) { rules.filter { it.matchesRuleSearch(query) } }
    }

@Composable
private fun ContentFilterSettingsEditor(
    state: SettingsUiState,
    actions: SettingsActions,
    id: String,
    close: () -> Unit,
) {
    val current = state.app.contentFilters.firstOrNull { it.id == id }
    val board = state.homeBoard ?: defaultBoardAddress()
    val base = remember(id, current, board) { current ?: newContentFilterRule(board).copy(boards = emptySet()) }
    ContentFilterEditor(
        rule = base,
        editorKey = base.id,
        onClose = close,
        onSave = { next ->
            actions.onUpdate { app ->
                app.withContentFilters(
                    if (app.contentFilters.any { it.id == next.id }) {
                        app.contentFilters.map { if (it.id == next.id) next else it }
                    } else {
                        app.contentFilters + next
                    },
                )
            }
            close()
        },
        onDelete =
            current?.let { rule ->
                {
                    actions.onUpdate { app ->
                        app.withContentFilters(app.contentFilters.filterNot { it.id == rule.id })
                    }
                    close()
                }
            },
    )
}

/** Меняет только флаг включения; порядок и остальные поля правил сохраняются. */
internal fun List<ContentFilterRule>.withRuleEnabled(
    id: String,
    enabled: Boolean,
): List<ContentFilterRule> = map { if (it.id == id && it.enabled != enabled) it.copy(enabled = enabled) else it }
