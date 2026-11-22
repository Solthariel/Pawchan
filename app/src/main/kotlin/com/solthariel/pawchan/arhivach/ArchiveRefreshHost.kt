package com.solthariel.pawchan.arhivach

import androidx.compose.runtime.Composable
import com.solthariel.pawchan.core.ui.BidirectionalPullRefreshBox
import com.solthariel.pawchan.core.ui.PullRefreshOptions
import com.solthariel.pawchan.ui.APP_REFRESH_BOTTOM_INSET

/** Общий viewport-индикатор не меняет геометрию списка при любом способе обновления. */
@Composable
internal fun ArchiveRefreshHost(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit,
) {
    BidirectionalPullRefreshBox(
        refreshing = refreshing,
        onRefresh = onRefresh,
        options = PullRefreshOptions(allowBottom = true, bottomInset = APP_REFRESH_BOTTOM_INSET),
        content = content,
    )
}
