package com.solthariel.pawchan.infrastructure.thread.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.withClip
import coil3.BitmapImage
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.thread.ThreadShotStyle
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

internal fun shotMediaHeight(
    attachmentCount: Int,
    mediaSize: Int,
): Int {
    if (attachmentCount == 0) return 0
    val rows = (attachmentCount + SHOT_MEDIA_COLUMNS - 1) / SHOT_MEDIA_COLUMNS
    return rows * mediaSize + (rows - 1) * SHOT_MEDIA_GAP
}

internal suspend fun drawShotMedia(
    context: Context,
    loader: ImageLoader,
    canvas: Canvas,
    attachments: List<MediaAttachmentUi>,
    style: ThreadShotStyle,
    mediaSize: Int,
) {
    attachments.forEachIndexed { index, attachment ->
        coroutineContext.ensureActive()
        val left = (index % SHOT_MEDIA_COLUMNS) * (mediaSize + SHOT_MEDIA_GAP)
        val top = (index / SHOT_MEDIA_COLUMNS) * (mediaSize + SHOT_MEDIA_GAP)
        drawShotAttachment(context, loader, canvas, ShotMediaCell(attachment, left, top, mediaSize), style)
    }
}

private suspend fun drawShotAttachment(
    context: Context,
    loader: ImageLoader,
    canvas: Canvas,
    cell: ShotMediaCell,
    style: ThreadShotStyle,
) {
    val bounds = RectF(cell.left.toFloat(), cell.top.toFloat(), cell.right.toFloat(), cell.bottom.toFloat())
    canvas.drawRoundRect(bounds, SHOT_MEDIA_RADIUS, SHOT_MEDIA_RADIUS, Paint().apply { color = style.border })
    val request =
        ImageRequest
            .Builder(context)
            .data(cell.attachment.thumbnailUrl)
            .size(cell.size)
            .allowHardware(false)
            .build()
    val result = loader.execute(request) as? SuccessResult
    val image = (result?.image as? BitmapImage)?.bitmap
    if (image != null) {
        val clip =
            Path().apply {
                addRoundRect(bounds, SHOT_MEDIA_RADIUS, SHOT_MEDIA_RADIUS, Path.Direction.CW)
            }
        canvas.withClip(clip) {
            val scale = maxOf(cell.size.toFloat() / image.width, cell.size.toFloat() / image.height)
            val drawnWidth = image.width * scale
            val drawnHeight = image.height * scale
            val left = bounds.centerX() - drawnWidth / 2
            val top = bounds.centerY() - drawnHeight / 2
            drawBitmap(
                image,
                null,
                RectF(left, top, left + drawnWidth, top + drawnHeight),
                Paint(Paint.FILTER_BITMAP_FLAG),
            )
        }
    }
    if (cell.attachment.viewer == AttachmentViewer.VIDEO) {
        drawShotPlayIcon(canvas, bounds)
    }
    drawShotMediaCaption(canvas, cell)
}

private fun drawShotMediaCaption(
    canvas: Canvas,
    cell: ShotMediaCell,
) {
    val captionHeight = SHOT_CAPTION_LINE_HEIGHT
    val top = cell.bottom - captionHeight
    canvas.drawRect(
        cell.left.toFloat(),
        top.toFloat(),
        cell.right.toFloat(),
        cell.bottom.toFloat(),
        Paint().apply { color = SHOT_CAPTION_BACKGROUND },
    )
    val availableWidth = (cell.size - SHOT_CAPTION_INSET * 2 - SHOT_CAPTION_GAP) / 2
    val baseline = top + SHOT_CAPTION_BASELINE
    drawShotCaptionText(
        canvas,
        cell.attachment.display.typeAndSize,
        cell.left + SHOT_CAPTION_INSET,
        baseline,
        availableWidth,
    )
    cell.attachment.displayDimensions?.let { dimensions ->
        drawShotCaptionText(
            canvas,
            dimensions,
            cell.right - SHOT_CAPTION_INSET,
            baseline,
            availableWidth,
            Paint.Align.RIGHT,
        )
    }
}

private fun drawShotCaptionText(
    canvas: Canvas,
    text: String,
    edge: Float,
    baseline: Float,
    availableWidth: Float,
    alignment: Paint.Align = Paint.Align.LEFT,
) {
    val paint = shotPaint(SHOT_CAPTION_TEXT_SIZE, Color.WHITE)
    val measuredWidth = paint.measureText(text)
    if (measuredWidth > availableWidth) paint.textSize *= availableWidth / measuredWidth
    paint.textAlign = alignment
    canvas.drawText(text, edge, baseline, paint)
}

private fun drawShotPlayIcon(
    canvas: Canvas,
    bounds: RectF,
) {
    val centerX = bounds.centerX()
    val centerY = bounds.centerY()
    val path =
        Path().apply {
            moveTo(centerX - SHOT_PLAY_SIZE / 2, centerY - SHOT_PLAY_SIZE)
            lineTo(centerX + SHOT_PLAY_SIZE, centerY)
            lineTo(centerX - SHOT_PLAY_SIZE / 2, centerY + SHOT_PLAY_SIZE)
            close()
        }
    canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
}

private data class ShotMediaCell(
    val attachment: MediaAttachmentUi,
    val left: Int,
    val top: Int,
    val size: Int,
) {
    val right: Int = left + size
    val bottom: Int = top + size
}

private const val SHOT_MEDIA_RADIUS = 16f
private const val SHOT_CAPTION_LINE_HEIGHT = 22
private const val SHOT_CAPTION_TEXT_SIZE = 16f
private const val SHOT_CAPTION_GAP = 4f
private const val SHOT_CAPTION_INSET = 6f
private const val SHOT_CAPTION_BASELINE = 17f
private const val SHOT_CAPTION_BACKGROUND = 0xB0000000.toInt()
private const val SHOT_PLAY_SIZE = 16f
