package com.solthariel.pawchan.catalog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.solthariel.pawchan.R
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_SUBTITLE_SIZE
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_TITLE_SIZE
import com.solthariel.pawchan.core.ui.UiDisclosureIndicator
import com.solthariel.pawchan.core.ui.UiToolbarSearchField
import com.solthariel.pawchan.domain.model.BoardAddress

@Composable
internal fun CatalogBoardIdentity(
    board: BoardAddress,
    label: CatalogBoardLabel,
    modifier: Modifier,
    switchable: Boolean = false,
    expanded: Boolean = false,
) {
    val title = label.title
    val boardLabel = if (title.isNullOrBlank()) "/${board.board.value}/" else "/${board.board.value}/ — $title"
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = boardLabel,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 22.sp),
            )
            val speed = label.speed?.takeIf { it > 0 }
            if (speed != null || label.speedSupported) {
                Text(
                    text = speed?.let { pluralStringResource(R.plurals.number_posts_per_hour, it, it) } ?: "\u00a0",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            fontSize = APP_TOOLBAR_SUBTITLE_SIZE,
                            lineHeight = 16.sp,
                        ),
                )
            }
        }
        if (switchable) UiDisclosureIndicator(expanded)
    }
}

@Composable
internal fun CatalogSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier,
) {
    UiToolbarSearchField(
        query,
        onQueryChange,
        focusRequester,
        modifier,
        stringResource(R.string.search),
        stringResource(R.string.clear_search),
    )
}
