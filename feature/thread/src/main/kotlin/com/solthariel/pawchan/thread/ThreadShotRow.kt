package com.solthariel.pawchan.thread

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import com.solthariel.pawchan.core.ui.FAST_SCROLL_CONTENT_INSET

/** The recording boundary includes the real gutters, marker and divider. Only selection UI is painted afterwards. */
@Composable
internal fun ThreadShotRow(
    id: Long,
    tools: ThreadToolsState,
    content: @Composable () -> Unit,
) {
    val recording = if (tools.selecting) rememberShotRecording(id, tools.shots) else null
    val selected = recording != null && id in tools.selected
    val modifier =
        if (recording == null) {
            Modifier
        } else {
            Modifier.drawWithContent {
                recording.layer.record {
                    drawRect(recording.background)
                    this@drawWithContent.drawContent()
                }
                drawLayer(recording.layer)
                tools.shots.recorded(id, recording.layer)
                if (selected) drawRect(recording.selectionTint)
            }
        }
    Column(modifier.fillMaxWidth().padding(horizontal = FAST_SCROLL_CONTENT_INSET)) { content() }
}

/** What a row draws into while posts can be selected for a shot; the layer is released with the row. */
private class ShotRowRecording(
    val layer: GraphicsLayer,
    val background: Color,
    val selectionTint: Color,
)

@Composable
private fun rememberShotRecording(
    id: Long,
    shots: ThreadShotCapture,
): ShotRowRecording {
    val layer = rememberGraphicsLayer()
    DisposableEffect(id, shots, layer) { onDispose { shots.detach(id, layer) } }
    return ShotRowRecording(
        layer = layer,
        background = MaterialTheme.colorScheme.background,
        selectionTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
    )
}
