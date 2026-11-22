package com.solthariel.pawchan.composer

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.core.graphics.withScale
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftImageMark
import com.solthariel.pawchan.domain.drafts.DraftShape
import com.solthariel.pawchan.domain.drafts.DraftStroke

@Composable
internal fun ImageLiveDrawingOverlay(
    edit: DraftImageEdit,
    mark: DraftImageMark,
    sourceSize: ReencodingDimensions,
    modifier: Modifier,
) {
    val dimensions = imageEditDimensions(sourceSize.width, sourceSize.height, edit, null)
    val space = ImageEditRasterSpace(sourceSize.width, sourceSize.height, dimensions.crop, edit)
    Canvas(modifier) {
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.withScale(size.width / dimensions.width, size.height / dimensions.height, 0f, 0f) {
                when (mark) {
                    is DraftStroke -> drawEditStroke(this, space, mark)
                    is DraftShape -> drawEditShape(this, space, mark)
                    else -> Unit
                }
            }
        }
    }
}
