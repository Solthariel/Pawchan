package com.solthariel.pawchan.catalog

import androidx.compose.runtime.Composable
import com.solthariel.pawchan.boards.BoardSwipeDirection

internal data class CatalogBoardLookupBindings(
    val visible: Boolean,
    val query: String,
    val onQueryChange: (String) -> Unit,
    val onOpen: () -> Unit,
    val onDismiss: () -> Unit,
    val onAdjacentForum: (BoardSwipeDirection) -> Unit = {},
    val suggestions: @Composable () -> Unit,
)
