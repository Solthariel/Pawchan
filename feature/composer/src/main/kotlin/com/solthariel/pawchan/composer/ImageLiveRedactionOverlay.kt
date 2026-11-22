package com.solthariel.pawchan.composer

import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftRedaction
import com.solthariel.pawchan.domain.drafts.DraftRedactionMode
import com.solthariel.pawchan.domain.drafts.DraftRedactionTool
import com.solthariel.pawchan.domain.drafts.sourceToCanvas

/** Pending redactions always cover old pixels, including while another stroke is awaiting rasterization. */
@Composable
internal fun ImagePendingMarkOverlay(
    edit: DraftImageEdit,
    mark: com.solthariel.pawchan.domain.drafts.DraftImageMark,
    sourceSize: ReencodingDimensions?,
    modifier: Modifier,
) {
    val size = sourceSize ?: return
    if (mark is DraftRedaction) {
        Canvas(modifier) {
            val path = liveRedactionPath(edit, mark, size.width, size.height, this.size.width, this.size.height)
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawPath(
                    path,
                    Paint().apply {
                        color =
                            if (mark.mode == DraftRedactionMode.FILL) mark.opaqueColor else android.graphics.Color.BLACK
                    },
                )
            }
        }
    } else {
        ImageLiveDrawingOverlay(edit, mark, size, modifier)
    }
}

private fun liveRedactionPath(
    edit: DraftImageEdit,
    mark: DraftRedaction,
    sourceWidth: Int,
    sourceHeight: Int,
    width: Float,
    height: Float,
): Path {
    val points =
        mark.points.map {
            val point = edit.sourceToCanvas(it)
            ImageEditPoint(point.x * width, point.y * height)
        }
    val outputWidth =
        if (edit.quarterTurns % 2 == 0) {
            sourceWidth * (edit.crop.right - edit.crop.left)
        } else {
            sourceHeight * (edit.crop.bottom - edit.crop.top)
        }
    val strokeWidth = (mark.width * minOf(sourceWidth, sourceHeight) * width / outputWidth).coerceAtLeast(1f)
    return when {
        mark.tool == DraftRedactionTool.RECTANGLE -> {
            Path().apply {
                addRect(editRect(points[0], points[1]), Path.Direction.CW)
            }
        }

        points.size == 1 -> {
            Path().apply {
                addCircle(points[0].x, points[0].y, strokeWidth / 2f, Path.Direction.CW)
            }
        }

        else -> {
            Path().also { editStrokePaint(android.graphics.Color.BLACK, strokeWidth).getFillPath(editPath(points), it) }
        }
    }
}
