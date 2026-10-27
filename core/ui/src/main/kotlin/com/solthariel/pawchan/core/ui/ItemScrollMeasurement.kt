package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun BindItemScrollMeasurement(
    state: LazyListState,
    session: ItemScrollSession,
    gesture: FastScrollGesture,
    mode: FastScrollMode,
) {
    LaunchedEffect(state, session, gesture, mode) {
        if (mode == FastScrollMode.ITEMS) {
            snapshotFlow { Triple(state.layoutInfo.totalItemsCount, state.layoutInfo.viewportSize, gesture.dragging) }
                .distinctUntilChanged()
                .collect { (count, _, dragging) -> if (count > 0 && !dragging) session.begin() }
        }
    }
}
