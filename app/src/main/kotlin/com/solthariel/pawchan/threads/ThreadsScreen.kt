package com.solthariel.pawchan.threads

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.catalog.CATALOG_BOTTOM_BAR_HEIGHT
import com.solthariel.pawchan.catalog.CatalogBottomActions
import com.solthariel.pawchan.catalog.CatalogBottomDestination
import com.solthariel.pawchan.catalog.CatalogIconButton
import com.solthariel.pawchan.catalog.CatalogNavigationWorkspace
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.domain.model.ThreadAddress

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
)

/** Экран тредов: страницы, избранные треды и ответы — сворачиваемые секции, ниже история и копии. */
@Composable
internal fun ThreadsScreen(
    input: ThreadsScreenInput,
    actions: ThreadsScreenActions,
    bottomActions: CatalogBottomActions,
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
    val counterWidth = rememberThreadCounterWidth(listOf(input.favorites.orEmpty(), input.replies))
    val unread = input.replies.sumOf { it.counter.count }
    LaunchedEffect(unread > 0) { if (unread > 0) sections.expand(ThreadsSection.REPLIES) }
    CatalogNavigationWorkspace(actions = bottomActions, selected = CatalogBottomDestination.THREADS) {
        Column(Modifier.fillMaxSize()) {
            ThreadsToolbar(input.refreshing, query, { query = it }, actions, input.pages.isNotEmpty()) {
                displaySettings = true
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = CATALOG_BOTTOM_BAR_HEIGHT + 8.dp),
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
                // Как в Dashchan: крестик в заголовке «Страниц» закрывает все страницы сразу, без вопроса.
                threadsSection(ThreadsSection.PAGES, visibleInput.pages, list, actions.pages.closeAll)
                threadsSection(ThreadsSection.FAVORITES, visibleInput.favorites, list)
                threadsSection(ThreadsSection.REPLIES, visibleInput.replies, list)
                item(key = "links") { ThreadsLinks(input.copies, actions) }
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
    /** Общая ширина ячеек счётчика в «Избранных тредах» и «Ответах». */
    val counterWidth: Dp,
    val searching: Boolean,
    val limits: com.solthariel.pawchan.domain.settings.NavigationPreferences,
    val onMenu: (ThreadsMenuTarget) -> Unit,
)

private fun LazyListScope.threadsSection(
    section: ThreadsSection,
    rows: List<ThreadRowUi>?,
    list: ThreadsListContext,
    onCloseAll: (() -> Unit)? = null,
) {
    val expanded = list.searching || section !in list.sections.collapsed
    val total = ThreadCounter(rows.orEmpty().sumOf { it.counter.count })
    item(key = "header-${section.name}", contentType = "threads-header") {
        val closeAll = onCloseAll?.takeIf { expanded && !rows.isNullOrEmpty() }
        ThreadsSectionHeader(
            section,
            expanded,
            total,
            { list.sections.toggle(section) },
            counterWidth = list.counterWidth.takeIf { section != ThreadsSection.PAGES },
            trailing = closeAll?.let { { CatalogIconButton(UiIconKind.CLOSE, "Закрыть все", onClick = it) } },
        )
    }
    if (expanded) {
        threadsSectionRows(section, rows, list)
    }
    item(key = "divider-${section.name}", contentType = "threads-divider") {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

internal fun ThreadsSection.emptyText(): String =
    when (this) {
        ThreadsSection.PAGES -> "Открытых тредов нет"
        ThreadsSection.FAVORITES -> "В избранном пока пусто"
        ThreadsSection.REPLIES -> "Здесь появятся треды, где вы писали"
    }

@Composable
private fun ThreadsLinks(
    copies: Int,
    actions: ThreadsScreenActions,
) {
    Column {
        ThreadsLinkRow(UiIconKind.DOCUMENT, "История", null, actions.links.history)
        ThreadsLinkRow(UiIconKind.SAVE, "Сохранённые копии", copies, actions.links.copies)
    }
}
