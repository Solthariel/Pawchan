package com.solthariel.pawchan.composer

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.solthariel.pawchan.domain.drafts.DraftCrop
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftPoint
import com.solthariel.pawchan.domain.drafts.sourceToCanvas

/** Keep the full editing plane fixed between tools; shade excluded pixels instead of refitting the image. */
@Composable
internal fun ImageCropPreviewMask(
    edit: DraftImageEdit,
    modifier: Modifier,
) {
    if (edit.crop == DraftCrop()) return
    val full = edit.copy(crop = DraftCrop())
    val from = full.sourceToCanvas(DraftPoint(edit.crop.left, edit.crop.top))
    val to = full.sourceToCanvas(DraftPoint(edit.crop.right, edit.crop.bottom))
    Canvas(modifier) {
        val left = minOf(from.x, to.x) * size.width
        val right = maxOf(from.x, to.x) * size.width
        val top = minOf(from.y, to.y) * size.height
        val bottom = maxOf(from.y, to.y) * size.height
        val shade = Color.Black.copy(alpha = .7f)
        drawRect(shade, Offset.Zero, Size(size.width, top))
        drawRect(shade, Offset(0f, bottom), Size(size.width, size.height - bottom))
        drawRect(shade, Offset(0f, top), Size(left, bottom - top))
        drawRect(shade, Offset(right, top), Size(size.width - right, bottom - top))
    }
}
