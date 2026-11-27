package com.solthariel.pawchan.thread

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.postContainerShape
import com.solthariel.pawchan.domain.model.ThreadAddress
import kotlinx.coroutines.launch
import com.solthariel.pawchan.feature.thread.R.string as L

@Composable
internal fun BindThreadReadingReturn(
    memory: ThreadScreenMemory,
    content: ThreadUiState.Content?,
    address: ThreadAddress?,
    reading: ThreadReadingSession?,
) {
    val res = LocalResources.current

    val tools = memory.tools
    val scope = rememberCoroutineScope()
    val latest by rememberUpdatedState(content)
    LaunchedEffect(reading, address, tools) {
        tools.readingReturn = null
        if (reading != null && address != null) {
            reading.returnPoint(address).collect { tools.readingReturn = it }
        }
    }
    SideEffect {
        tools.onReadingReturn = {
            val point = tools.readingReturn
            val index = point?.let { latest?.presentation?.postPositions?.get(it.postId) }
            when {
                index == null -> {
                    tools.notice = res.getString(L.ui_reading_position_unavailable_in_loaded)
                }

                address != null && reading != null -> {
                    scope.launch {
                        memory.list.scrollToItem(index, point.offset)
                        reading.clearPendingPost(address)
                        reading.returned(address, point)
                    }
                }
            }
        }
    }
}

/** Draw-only marker: inserting a label row would change the saved pixel offset. */
@Composable
internal fun Modifier.readingReturnMarker(marked: Boolean): Modifier {
    val res = LocalResources.current

    val color = MaterialTheme.colorScheme.secondary
    val shape = postContainerShape()
    return if (marked) {
        semantics { stateDescription = res.getString(L.ui_reading_position) }.drawWithCache {
            val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
            val stroke = 2.dp.toPx()
            onDrawWithContent {
                drawContent()
                clipPath(path) {
                    drawLine(color, Offset(0f, stroke / 2), Offset(size.width, stroke / 2), strokeWidth = stroke)
                }
            }
        }
    } else {
        this
    }
}
