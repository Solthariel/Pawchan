package com.solthariel.pawchan.catalog

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.APP_TOP_BAR_HEIGHT

/** The pointer observer is an ancestor of the list, not a sibling intercepting its hit path. */
@Composable
internal fun CatalogBoardLookupLayer(
    lookup: CatalogBoardLookupBindings?,
    content: @Composable () -> Unit,
) {
    var panelBounds by remember { mutableStateOf(Rect.Zero) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val currentLookup = rememberUpdatedState(lookup)
    val bounds = rememberUpdatedState(panelBounds)
    val rootOrigin = rememberUpdatedState(origin)
    val toolbarHeight = with(LocalDensity.current) { APP_TOP_BAR_HEIGHT.toPx() }
    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInRoot() }
            .pointerInput(Unit) {
                observeBoardLookupDismissal(currentLookup, bounds, rootOrigin, toolbarHeight)
            },
    ) {
        content()
        if (lookup?.visible == true) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = APP_TOP_BAR_HEIGHT, start = 8.dp, end = 8.dp)
                        .onGloballyPositioned { panelBounds = it.boundsInRoot() }
                        .testTag("board-lookup-panel"),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = 4.dp,
            ) {
                Box(Modifier.pointerInput(Unit) { detectTapGestures {} }) { lookup.suggestions() }
            }
        }
    }
}
