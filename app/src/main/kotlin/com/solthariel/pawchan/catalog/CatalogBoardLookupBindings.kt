package com.solthariel.pawchan.catalog

import androidx.compose.runtime.Composable

internal data class CatalogBoardLookupBindings(
    val visible: Boolean,
    val query: String,
    val onQueryChange: (String) -> Unit,
    val onOpen: () -> Unit,
    val onDismiss: () -> Unit,
    val suggestions: @Composable () -> Unit,
)
