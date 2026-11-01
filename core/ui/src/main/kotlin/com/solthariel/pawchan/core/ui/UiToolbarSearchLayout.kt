package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Geometry only: each caller retains its query, focus, IME actions and search owner. */
@Composable
fun UiToolbarSearchLayout(
    modifier: Modifier,
    onClear: (() -> Unit)?,
    clearDescription: String = "Очистить поиск",
    input: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier
            .heightIn(min = APP_TOOLBAR_BUTTON_SIZE)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = SEARCH_ALPHA), RoundedCornerShape(24.dp))
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UiIcon(UiIconKind.SEARCH, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(20.dp))
        Spacer(Modifier.size(8.dp))
        input()
        if (onClear != null) {
            UiToolbarButton(UiIconKind.CLOSE, clearDescription, onClick = onClear)
        } else {
            Spacer(Modifier.size(12.dp))
        }
    }
}

private const val SEARCH_ALPHA = 0.58f
