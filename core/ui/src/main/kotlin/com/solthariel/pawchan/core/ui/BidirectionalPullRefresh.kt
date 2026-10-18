package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun BidirectionalPullRefreshBox(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    options: PullRefreshOptions,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val gesture = remember { PullRefreshGesture() }
    val latestRefresh = rememberUpdatedState(refreshing)
    val latestAction = rememberUpdatedState(onRefresh)
    val tablet = LocalConfiguration.current.smallestScreenWidthDp >= TABLET_MINIMUM_WIDTH
    val connection =
        remember(gesture, options.allowBottom) {
            pullRefreshConnection(
                gesture,
                options.allowBottom,
                busy = { latestRefresh.value || gesture.requested },
                request = { latestAction.value() },
            )
        }
    ObserveRefreshCompletion(gesture, refreshing)
    val visual = rememberPullRefreshVisual(gesture.release)
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .clipToBounds()
                .onSizeChanged { gesture.updateHeight(it.height, tablet) }
                .nestedScroll(connection),
    ) {
        content()
        PullRefreshIndicator(gesture, visual, options.bottomInset, Modifier.matchParentSize())
    }
}

@Composable
private fun ObserveRefreshCompletion(
    gesture: PullRefreshGesture,
    refreshing: Boolean,
) {
    val latestRefresh = rememberUpdatedState(refreshing)
    LaunchedEffect(refreshing, gesture.requestId) {
        if (gesture.requested) {
            if (refreshing) {
                gesture.refreshingSeen = true
            } else if (gesture.refreshingSeen) {
                gesture.complete()
            } else {
                // A cache/429 result can complete before a frame observes refreshing=true.
                delay(QUICK_REFRESH_SETTLE_MILLIS)
                if (!latestRefresh.value) gesture.complete()
            }
        }
    }
}

private fun pullRefreshConnection(
    gesture: PullRefreshGesture,
    allowBottom: Boolean,
    busy: () -> Boolean,
    request: () -> Unit,
): NestedScrollConnection =
    object : NestedScrollConnection {
        override fun onPreScroll(
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            if (source != NestedScrollSource.UserInput || busy()) return Offset.Zero
            return Offset(0f, gesture.consumeClosing(available.y))
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            if (source != NestedScrollSource.UserInput || busy()) return Offset.Zero
            return Offset(0f, gesture.consumeEdge(available.y, allowBottom))
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            val pulled = gesture.distance != 0f
            if (gesture.release(busy())) request()
            return if (pulled) Velocity(0f, available.y) else Velocity.Zero
        }
    }

private const val QUICK_REFRESH_SETTLE_MILLIS = 100L
private const val TABLET_MINIMUM_WIDTH = 600

data class PullRefreshOptions(
    val allowBottom: Boolean,
    val bottomInset: Dp = 0.dp,
)
