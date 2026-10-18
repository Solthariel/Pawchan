package com.solthariel.pawchan.catalog

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.solthariel.pawchan.R
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_SUBTITLE_SIZE
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_TITLE_SIZE
import com.solthariel.pawchan.core.ui.UiToolbarSearchField
import com.solthariel.pawchan.domain.model.BoardAddress

@Composable
internal fun CatalogBoardIdentity(
    board: BoardAddress,
    title: String?,
    boardSpeed: Int?,
    modifier: Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text =
                if (title.isNullOrBlank()) {
                    "/${board.board.value}/"
                } else {
                    "/${board.board.value}/ — $title"
                },
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontSize = APP_TOOLBAR_TITLE_SIZE,
                    lineHeight = 18.sp,
                ),
        )
        boardSpeed?.takeIf { speed -> speed > 0 }?.let { speed ->
            Text(
                text =
                    pluralStringResource(
                        R.plurals.number_posts_per_hour,
                        speed,
                        speed,
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        fontSize = APP_TOOLBAR_SUBTITLE_SIZE,
                        lineHeight = 12.sp,
                    ),
            )
        }
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
