package com.solthariel.pawchan.arhivach

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.catalog.CatalogBoardLookupBindings
import com.solthariel.pawchan.catalog.CatalogBoardLookupToolbar
import com.solthariel.pawchan.catalog.CatalogSearchBar
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_HORIZONTAL_INSET
import com.solthariel.pawchan.core.ui.APP_TOP_BAR_HEIGHT
import com.solthariel.pawchan.core.ui.UiDisclosureIndicator
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiRefreshButton
import com.solthariel.pawchan.core.ui.UiToolbarButton
import com.solthariel.pawchan.core.ui.UiToolbarSearchTransition
import com.solthariel.pawchan.domain.archive.ArchiveTag

@Composable
internal fun ArchiveToolbar(
    state: ArchiveBrowseState,
    actions: ArchiveToolbarActions,
    searchBindings: ArchiveToolbarSearchBindings,
    galleryEnabled: Boolean = false,
) {
    var menuVisible by rememberSaveable { mutableStateOf(false) }
    if (searchBindings.lookup.visible) {
        CatalogBoardLookupToolbar(searchBindings.lookup, "Введите тег…")
    } else if (state.selectedThread != null) {
        ArchiveThreadToolbar(state, actions, searchBindings.words, galleryEnabled)
    } else {
        ArchiveBrowseToolbar(state, actions, searchBindings, menuVisible) { menuVisible = it }
    }
}

@Composable
private fun ArchiveBrowseToolbar(
    state: ArchiveBrowseState,
    actions: ArchiveToolbarActions,
    searchBindings: ArchiveToolbarSearchBindings,
    menuVisible: Boolean,
    onMenuVisibilityChange: (Boolean) -> Unit,
) {
    val pageSearchVisible = searchBindings.words.visible
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val closePageSearch: () -> Unit = {
        searchBindings.words.setVisible(false)
        searchBindings.words.setQuery("")
        focusManager.clearFocus()
        keyboard?.hide()
        Unit
    }
    BackHandler(enabled = pageSearchVisible, onBack = closePageSearch)
    LaunchedEffect(pageSearchVisible) {
        if (pageSearchVisible) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = APP_TOP_BAR_HEIGHT)
            .padding(horizontal = APP_TOOLBAR_HORIZONTAL_INSET),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UiToolbarButton(UiIconKind.BACK, if (pageSearchVisible) "Закрыть поиск" else "Назад") {
            if (pageSearchVisible) {
                closePageSearch()
            } else {
                actions.onBack()
            }
        }
        ArchiveListToolbarContent(
            state,
            actions,
            searchBindings,
            menuVisible,
            onMenuVisibilityChange,
            focusRequester,
        )
    }
}

@Composable
private fun RowScope.ArchiveListToolbarContent(
    state: ArchiveBrowseState,
    actions: ArchiveToolbarActions,
    searchBindings: ArchiveToolbarSearchBindings,
    menuVisible: Boolean,
    onMenuVisibilityChange: (Boolean) -> Unit,
    focusRequester: FocusRequester,
) {
    UiToolbarSearchTransition(
        searchVisible = searchBindings.words.visible,
        modifier = Modifier.weight(1f),
        label = "archive-page-search",
    ) { searchActive ->
        if (searchActive) {
            CatalogSearchBar(
                query = searchBindings.words.query,
                onQueryChange = searchBindings.words.setQuery,
                focusRequester = focusRequester,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            )
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .clickable { searchBindings.lookup.onOpen() }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Архивач", style = MaterialTheme.typography.titleMedium)
                    Text(
                        state.tags.takeIf { it.isNotEmpty() }?.joinToString(" + ") { it.title } ?: "Выбрать тег",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                UiDisclosureIndicator(expanded = false)
            }
        }
    }
    if (!searchBindings.words.visible) {
        UiToolbarButton(UiIconKind.SEARCH, "Поиск по словам") { searchBindings.words.setVisible(true) }
    }
    ArchiveRefreshButton(state, actions.model)
    Box {
        UiToolbarButton(UiIconKind.MORE, "Меню страниц") { onMenuVisibilityChange(true) }
        ArchivePageMenu(state, actions.model, menuVisible) { onMenuVisibilityChange(false) }
    }
}

@Composable
private fun ArchiveRefreshButton(
    state: ArchiveBrowseState,
    model: ArchiveBrowseModel,
) {
    UiRefreshButton(
        isRefreshing = state.load.loading,
        onClick = model::refresh,
        description = "Обновить",
        enabled = !state.load.loading,
    )
}

internal data class ArchiveToolbarActions(
    val model: ArchiveBrowseModel,
    val onBack: () -> Unit,
    val onThreadBack: (() -> Unit)? = null,
    val onGalleryClick: () -> Unit,
)

internal data class ArchiveToolbarSearchBindings(
    val lookup: CatalogBoardLookupBindings,
    val words: ArchiveWordSearch,
)

@Composable
internal fun ArchiveTagSuggestions(
    state: ArchiveBrowseState,
    model: ArchiveBrowseModel,
    close: () -> Unit,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(max = ARCHIVE_TAG_SUGGESTION_ROW_HEIGHT * ARCHIVE_TAG_SUGGESTION_MAX_ROWS)
                .testTag("archive-tag-suggestions"),
    ) {
        if (state.search.error) {
            item { ArchiveTagSuggestionMessage("Не удалось найти теги") }
        }
        if (state.search.query.isBlank()) {
            item { ArchiveTagSuggestionMessage("Введите название тега") }
        }
        items(state.search.suggestions, key = { it.id }) { tag ->
            val selected = state.tags.any { it.id == tag.id }
            ArchiveTagSuggestionRow(
                tag = tag,
                enabled = !selected && state.tags.size < MAX_SELECTED_TAGS,
                onClick = {
                    model.selectTag(tag)
                    close()
                },
            )
        }
    }
}

@Composable
private fun ArchiveTagSuggestionRow(
    tag: ArchiveTag,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(ARCHIVE_TAG_SUGGESTION_ROW_HEIGHT)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag("archive-tag-row-${tag.id}")
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            tag.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color =
                if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ArchiveTagSuggestionMessage(message: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(ARCHIVE_TAG_SUGGESTION_ROW_HEIGHT)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private const val ARCHIVE_TAG_SUGGESTION_MAX_ROWS = 4
private val ARCHIVE_TAG_SUGGESTION_ROW_HEIGHT = 48.dp
