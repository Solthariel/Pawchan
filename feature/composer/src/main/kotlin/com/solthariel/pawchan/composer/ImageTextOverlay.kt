package com.solthariel.pawchan.composer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.solthariel.pawchan.domain.drafts.ImageCanvasPoint

/** A tap selects the output position; the text is committed only after the dialog confirms. */
@Composable
internal fun ImageTextOverlay(
    onPlace: (ImageCanvasPoint) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnPlace by rememberUpdatedState(onPlace)
    Canvas(
        modifier.semantics { contentDescription = "Поле текста" }.pointerInput(Unit) {
            detectTapGestures { position ->
                if (size.width > 0 && size.height > 0) {
                    val x = (position.x / size.width).coerceIn(0f, 1f)
                    val y = (position.y / size.height).coerceIn(0f, 1f)
                    currentOnPlace(ImageCanvasPoint(x, y))
                }
            }
        },
    ) {}
}
