package com.solthariel.pawchan.media

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind

@Composable
internal fun GalleryIconButton(
    description: String,
    action: () -> Unit,
    content: @Composable () -> Unit,
) {
    IconButton(onClick = action, modifier = Modifier.semantics { contentDescription = description }) { content() }
}

@Composable
internal fun GalleryFilterIcon(
    filter: GalleryFilter,
    color: Color,
) {
    if (filter == GalleryFilter.GIF) {
        Text("GIF", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    } else if (filter == GalleryFilter.ALL) {
        UiIcon(UiIconKind.GRID, color, Modifier.size(18.dp))
    } else {
        Canvas(Modifier.size(18.dp)) {
            val stroke = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
            if (filter == GalleryFilter.IMAGES) drawImageFilter(color, stroke) else drawVideoFilter(color, stroke)
        }
    }
}

@Composable
internal fun GallerySelectIcon(color: Color) {
    Canvas(Modifier.size(24.dp)) {
        val stroke = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
        val path =
            Path().apply {
                moveTo(size.width * SELECT_START_X, size.height * SELECT_START_Y)
                lineTo(size.width * SELECT_MIDDLE_X, size.height * SELECT_MIDDLE_Y)
                lineTo(size.width * SELECT_END_X, size.height * SELECT_END_Y)
            }
        drawPath(path, color, style = stroke)
        drawRoundRect(
            color,
            topLeft = Offset(size.width * SELECT_BOX_INSET, size.height * SELECT_BOX_INSET),
            size = Size(size.width * SELECT_BOX_SIZE, size.height * SELECT_BOX_SIZE),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = stroke,
        )
    }
}

private fun DrawScope.drawImageFilter(
    color: Color,
    stroke: Stroke,
) {
    drawRoundRect(color, cornerRadius = CornerRadius(2.dp.toPx()), style = stroke)
    val path =
        Path().apply {
            moveTo(size.width * IMAGE_START_X, size.height * IMAGE_START_Y)
            lineTo(size.width * IMAGE_FIRST_X, size.height * IMAGE_FIRST_Y)
            lineTo(size.width * IMAGE_SECOND_X, size.height * IMAGE_SECOND_Y)
            lineTo(size.width * IMAGE_THIRD_X, size.height * IMAGE_THIRD_Y)
            lineTo(size.width * IMAGE_END_X, size.height * IMAGE_END_Y)
        }
    drawPath(path, color, style = stroke)
}

private fun DrawScope.drawVideoFilter(
    color: Color,
    stroke: Stroke,
) {
    drawRoundRect(color, cornerRadius = CornerRadius(2.dp.toPx()), style = stroke)
    val triangle =
        Path().apply {
            moveTo(size.width * VIDEO_START_X, size.height * VIDEO_START_Y)
            lineTo(size.width * VIDEO_END_X, size.height * VIDEO_CENTER_Y)
            lineTo(size.width * VIDEO_START_X, size.height * VIDEO_END_Y)
            close()
        }
    drawPath(triangle, color)
}

private const val SELECT_START_X = 0.28f
private const val SELECT_START_Y = 0.56f
private const val SELECT_MIDDLE_X = 0.43f
private const val SELECT_MIDDLE_Y = 0.71f
private const val SELECT_END_X = 0.73f
private const val SELECT_END_Y = 0.34f
private const val SELECT_BOX_INSET = 0.12f
private const val SELECT_BOX_SIZE = 0.76f
private const val IMAGE_START_X = 0.12f
private const val IMAGE_START_Y = 0.76f
private const val IMAGE_FIRST_X = 0.4f
private const val IMAGE_FIRST_Y = 0.46f
private const val IMAGE_SECOND_X = 0.58f
private const val IMAGE_SECOND_Y = 0.63f
private const val IMAGE_THIRD_X = 0.75f
private const val IMAGE_THIRD_Y = 0.45f
private const val IMAGE_END_X = 0.9f
private const val IMAGE_END_Y = 0.64f
private const val VIDEO_START_X = 0.42f
private const val VIDEO_START_Y = 0.3f
private const val VIDEO_END_X = 0.72f
private const val VIDEO_CENTER_Y = 0.5f
private const val VIDEO_END_Y = 0.7f
