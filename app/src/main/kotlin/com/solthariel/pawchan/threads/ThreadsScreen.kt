package com.solthariel.pawchan.threads

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.navigation.chrome.AppBottomDestination
import com.solthariel.pawchan.navigation.chrome.AppNavigationWorkspace
import com.solthariel.pawchan.R.string as L

internal data class ThreadsScreenInput(
    val pages: List<ThreadRowUi>,
    /** null — избранное ещё загружается. */
    val favorites: List<ThreadRowUi>?,
    val replies: List<ThreadRowUi>,
    /** Тред, из которого пришли, подсвечивается в «Страницах». */
    val current: ThreadAddress?,
    val favoriteAddresses: Set<ThreadAddress>,
    val copies: Int,
    val refreshing: Boolean,
)

internal data class ThreadsScreenActions(
    val open: (ThreadsSection, ThreadRowUi) -> Unit,
    val pages: ThreadsPageActions,
    val setFavorite: (ThreadRowUi, Boolean) -> Unit,
    val dismissReplies: (ThreadAddress) -> Unit,
    val copyLink: (ThreadAddress) -> Unit,
    val refresh: () -> Unit,
    val links: ThreadsLinkActions,
)

internal data class ThreadsPageActions(
    val close: (ThreadAddress) -> Unit,
    val closeAll: () -> Unit,
)

internal data class ThreadsLinkActions(
    val history: () -> Unit,
    val copies: () -> Unit,
    val downloads: () -> Unit = {},
)

/**
 * Экран тредов: страницы, избранные треды и ответы — сворачиваемые секции, ссылки на историю, копии и загрузки.
 * Порядок и то, какие ссылки убраны в ⋮, задаются в «⋮ → Отображение списка»; по умолчанию — как было.
 */
@Composable
internal fun ThreadsScreen(
    input: ThreadsScreenInput,
    actions: ThreadsScreenActions,
    bottomActions: AppBottomActions,
    onBack: () -> Unit,
    limits: com.solthariel.pawchan.domain.settings.NavigationPreferences =
        com.solthariel.pawchan.domain.settings
            .NavigationPreferences(),
    onLimitsChange: (com.solthariel.pawchan.domain.settings.NavigationPreferences) -> Unit = {},
) {
    var displaySettings by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf<String?>(null) }
    val searching = query != null
    val term = query.orEmpty().trim()

    fun List<ThreadRowUi>.matching() = filter { term.isEmpty() || it.title.contains(term, ignoreCase = true) }
    val visibleInput =
        input.copy(
            pages = input.pages.matching(),
            favorites = input.favorites?.matching(),
            replies = input.replies.matching(),
        )
    val sections = rememberThreadsSectionState()
    var menu by remember { mutableStateOf<ThreadsMenuTarget?>(null) }
    val counterWidth = rememberThreadCounterWidth(listOf(input.pages, input.favorites.orEmpty(), input.replies))
    val unread = input.replies.sumOf { it.counter.count }
    LaunchedEffect(unread > 0) { if (unread > 0) sections.expand(ThreadsSection.REPLIES) }
    AppNavigationWorkspace(actions = bottomActions, selected = AppBottomDestination.THREADS, floatingTop = true) {
        com.solthariel.pawchan.core.ui.FloatingTopChrome(toolbar = {
            ThreadsToolbar(
                input.refreshing,
                query,
                { query = it },
                actions,
                input.pages.isNotEmpty(),
                ThreadsToolbarActions(onBack, threadsMenuItems(limits.layout)) { displaySettings = true },
            )
        }) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        top = com.solthariel.pawchan.core.ui.LocalTopOverlayInset.current,
                        bottom = com.solthariel.pawchan.core.ui.LocalBottomOverlayInset.current + 8.dp,
                    ),
            ) {
                val list =
                    ThreadsListContext(
                        visibleInput,
                        sections,
                        actions,
                        counterWidth,
                        searching,
                        limits,
                        { menu = it },
                    )
                threadsHubContent(threadsScreenItems(limits.layout), list)
            }
        }
    }
    menu?.let { target -> ThreadsRowMenu(target, input.favoriteAddresses, actions) { menu = null } }
    if (displaySettings) ThreadsDisplayDialog(limits, onLimitsChange) { displaySettings = false }
}

internal data class ThreadsMenuTarget(
    val section: ThreadsSection,
    val row: ThreadRowUi,
)

internal class ThreadsListContext(
    val input: ThreadsScreenInput,
    val sections: ThreadsSectionState,
    val actions: ThreadsScreenActions,
    /** Общая ширина ячеек счётчика во всех трёх секциях. */
    val counterWidth: Dp,
    val searching: Boolean,
    val limits: com.solthariel.pawchan.domain.settings.NavigationPreferences,
    val onMenu: (ThreadsMenuTarget) -> Unit,
)

/** Разделы в выбранном порядке; ссылки перед секцией отделяются той же чертой, что и секции. */
private fun LazyListScope.threadsHubContent(
    items: List<com.solthariel.pawchan.domain.settings.ThreadsHubItem>,
    list: ThreadsListContext,
) {
    items.forEachIndexed { index, item ->
        val section = item.section()
        if (section != null) {
            threadsSection(section, list.input.rows(section), list)
        } else {
            item(key = "link-${item.id}", contentType = "threads-link") {
                ThreadsLinkItem(item, list.input.copies, list.actions.links)
            }
            if (items.getOrNull(index + 1)?.section() != null) {
                item(key = "link-divider-${item.id}", contentType = "threads-divider") { ThreadsSectionDivider() }
            }
        }
    }
}

private fun ThreadsScreenInput.rows(section: ThreadsSection): List<ThreadRowUi>? =
    when (section) {
        ThreadsSection.PAGES -> pages
        ThreadsSection.FAVORITES -> favorites
        ThreadsSection.REPLIES -> replies
    }

private fun LazyListScope.threadsSection(
    section: ThreadsSection,
    rows: List<ThreadRowUi>?,
    list: ThreadsListContext,
) {
    val expanded = list.searching || section !in list.sections.collapsed
    val total = threadsSectionCounter(section, rows)
    item(key = "header-${section.name}", contentType = "threads-header") {
        ThreadsSectionHeader(
            section,
            expanded,
            total,
            if (list.searching) null else ({ list.sections.toggle(section) }),
            counterWidth = list.counterWidth,
        )
    }
    if (expanded) {
        threadsSectionRows(section, rows, list)
    }
    item(key = "divider-${section.name}", contentType = "threads-divider") {
        ThreadsSectionDivider()
    }
}

@Composable
internal fun ThreadsSectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 8.dp).testTag("threads-section-divider"),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

internal fun ThreadsSection.emptyText(): String =
    when (this) {
        ThreadsSection.PAGES -> "Открытых тредов нет"
        ThreadsSection.FAVORITES -> "В избранном пока пусто"
        ThreadsSection.REPLIES -> "Здесь появятся треды, где вы писали"
    }

@Composable
private fun ThreadsLinkItem(
    item: com.solthariel.pawchan.domain.settings.ThreadsHubItem,
    copies: Int,
    links: ThreadsLinkActions,
) {
    val res = LocalResources.current
    val count = if (item == com.solthariel.pawchan.domain.settings.ThreadsHubItem.COPIES) copies else null
    ThreadsLinkRow(item.linkIcon(), res.threadsHubLabel(item), count) { links.open(item) }
}
