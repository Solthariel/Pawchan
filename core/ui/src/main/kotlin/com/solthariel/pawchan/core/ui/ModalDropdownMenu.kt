package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier

/** Keeps Material's anchor positioning and focusable popup; only the backdrop is shared. */
@Composable
fun ModalDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = LocalModalLayers.current
    val token = if (expanded && state != null) rememberModalEntry(state, ModalLayerKind.POPUP) else null
    CompositionLocalProvider(LocalModalToken provides (token ?: LocalModalToken.current)) {
        DropdownMenu(
            expanded,
            onDismissRequest,
            modifier.modalLayerSurface(MaterialTheme.shapes.extraSmall),
            scrollState = scrollState,
            content = content,
        )
    }
}
