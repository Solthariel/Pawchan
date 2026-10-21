package com.solthariel.pawchan.thread

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.model.ThreadAddress
import kotlinx.coroutines.launch

@Composable
internal fun BindThreadReadingReturn(
    memory: ThreadScreenMemory,
    content: ThreadUiState.Content?,
    address: ThreadAddress?,
    reading: ThreadReadingSession?,
) {
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
                    tools.notice = "Место чтения недоступно в загруженном треде"
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
    val color = MaterialTheme.colorScheme.secondary
    return if (marked) {
        semantics { stateDescription = "Место чтения" }.drawWithContent {
            drawContent()
            drawLine(color, Offset.Zero, Offset(size.width, 0f), strokeWidth = 2.dp.toPx())
        }
    } else {
        this
    }
}
