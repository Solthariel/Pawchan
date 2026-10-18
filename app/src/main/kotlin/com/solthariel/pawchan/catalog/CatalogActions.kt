package com.solthariel.pawchan.catalog

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.media.MediaAttachmentUi

internal data class CatalogActions(
    val content: CatalogContentActions,
    val navigation: CatalogNavigationActions,
    val threadMenuHost: CatalogThreadMenuHost? = null,
    val onToggleFiltered: () -> Unit = {},
    val onAddRule: ((CatalogThreadUi) -> Unit)? = null,
) {
    val onThreadClick: (ThreadAddress) -> Unit
        get() = content.onThreadClick
    val onMediaClick: (MediaAttachmentUi) -> Unit
        get() = content.onMediaClick
    val onRefresh: () -> Unit
        get() = content.onRefresh
    val onSearchQueryChange: (String) -> Unit
        get() = content.onSearchQueryChange
    val onChallenge: (String) -> Unit
        get() = content.onChallenge
    val onLibraryClick: () -> Unit
        get() = navigation.onLibraryClick
    val onSettingsClick: () -> Unit
        get() = navigation.onSettingsClick
    val onNewThreadClick: (BoardAddress) -> Unit
        get() = navigation.onNewThreadClick
    val onBoardSelected: (BoardId) -> Unit
        get() = navigation.onBoardSelected
}

internal data class CatalogContentActions(
    val onThreadClick: (ThreadAddress) -> Unit,
    val onMediaClick: (MediaAttachmentUi) -> Unit,
    val onRefresh: () -> Unit,
    val onSearchQueryChange: (String) -> Unit,
    val onChallenge: (String) -> Unit,
    /** Долгое нажатие на миниатюру (в карточке или в окне выбора файлов) — меню файла. */
    val onMediaLongClick: (MediaAttachmentUi) -> Unit = {},
)

internal data class CatalogNavigationActions(
    val onLibraryClick: () -> Unit,
    val onSettingsClick: () -> Unit,
    val onNewThreadClick: (BoardAddress) -> Unit,
    val onBoardSelected: (BoardId) -> Unit,
)

internal data class BoardsActions(
    val onFavoriteChanged: (BoardId, Boolean) -> Unit,
    val onCategoryExpanded: (String, Boolean) -> Unit,
    val onRetry: () -> Unit,
)
