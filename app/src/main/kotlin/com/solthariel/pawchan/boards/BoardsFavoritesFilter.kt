package com.solthariel.pawchan.boards

import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.R
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_BUTTON_SIZE
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind

@Composable
internal fun BoardsFavoritesFilter(
    favoritesOnly: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description =
        stringResource(
            if (favoritesOnly) R.string.boards_show_all else R.string.boards_show_favorites,
        )
    IconButton(
        onClick = onToggle,
        modifier =
            modifier
                .size(APP_TOOLBAR_BUTTON_SIZE)
                .semantics {
                    contentDescription = description
                    selected = favoritesOnly
                }.testTag("boards-favorites-filter"),
    ) {
        UiIcon(
            if (favoritesOnly) UiIconKind.BOOKMARK_FILLED else UiIconKind.BOOKMARK_ADD,
            if (favoritesOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            Modifier.size(24.dp),
        )
    }
}
