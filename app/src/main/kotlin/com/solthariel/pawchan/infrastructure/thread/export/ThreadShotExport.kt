package com.solthariel.pawchan.infrastructure.thread.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.StaticLayout
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import coil3.ImageLoader
import com.solthariel.pawchan.thread.ThreadExportPost
import com.solthariel.pawchan.thread.ThreadShotStyle
import kotlinx.coroutines.ensureActive
import java.io.IOException
import java.io.OutputStream
import kotlin.coroutines.coroutineContext

internal suspend fun renderThreadShot(
    context: Context,
    loader: ImageLoader,
    posts: List<ThreadExportPost>,
    style: ThreadShotStyle,
    destination: OutputStream,
) {
    val screenWidthDp =
        context.resources.configuration.screenWidthDp
            .takeIf { it > 0 } ?: DEFAULT_SHOT_WIDTH_DP
    val width = (screenWidthDp * SHOT_SCALE).toInt()
    val cardWidth = width - SHOT_OUTER_WIDTH
    val fontScale = context.resources.configuration.fontScale
    val blocks = prepareShotBlocks(posts, style, cardWidth, fontScale)
    val contentHeight = blocks.sumOf { it.height.toLong() }
    val height = if (blocks.isEmpty()) 0L else contentHeight + SHOT_CARD_GAP
    validateShotSize(height, width)
    val bitmap = createBitmap(width, height.toInt())
    try {
        val canvas = Canvas(bitmap)
        canvas.drawColor(style.background)
        canvas.translate(0f, SHOT_CARD_GAP.toFloat())
        for (block in blocks) {
            coroutineContext.ensureActive()
            drawShotBlock(context, loader, canvas, block, style, cardWidth)
            canvas.translate(0f, block.height.toFloat())
        }
        writeShot(bitmap, destination)
    } finally {
        bitmap.recycle()
    }
}

private fun prepareShotBlock(
    post: ThreadExportPost,
    style: ThreadShotStyle,
    cardWidth: Int,
    fontScale: Float,
): ThreadShotBlock {
    val width = cardWidth - SHOT_CARD_PADDING * 2
    val headerText = shotHeader(post, style)
    val header =
        staticShotLayout(
            headerText,
            shotPaint(SHOT_HEADER_SIZE * fontScale, style.secondaryText),
            width,
            SHOT_HEADER_LINE_HEIGHT * fontScale,
        )
    val bodyText = shotBody(post, style)
    val body =
        staticShotLayout(
            bodyText,
            shotPaint(style.textSizeSp * SHOT_SCALE * fontScale, style.text),
            width,
            (style.textSizeSp + SHOT_LINE_HEIGHT_EXTRA) * SHOT_SCALE * fontScale,
        )
    val subject =
        post.subject?.let {
            staticShotLayout(
                it,
                shotPaint(style.textSizeSp * SHOT_SCALE * fontScale, style.text, bold = true),
                width,
                (style.textSizeSp + SHOT_LINE_HEIGHT_EXTRA) * SHOT_SCALE * fontScale,
            )
        }
    val mediaSize = shotMediaSize(width)
    val mediaHeight = shotMediaHeight(post.attachments.size, mediaSize)
    val sections = listOf(mediaHeight, subject?.height ?: 0, body.height).count { it > 0 }
    val contentHeight = header.height + mediaHeight + (subject?.height ?: 0) + body.height
    val cardHeight = SHOT_CARD_PADDING * 2 + contentHeight + sections * SHOT_SECTION_GAP
    return ThreadShotBlock(post, header, body, mediaHeight, subject, cardHeight)
}

private suspend fun drawShotBlock(
    context: Context,
    loader: ImageLoader,
    canvas: Canvas,
    block: ThreadShotBlock,
    style: ThreadShotStyle,
    cardWidth: Int,
) {
    drawShotCard(canvas, block.cardHeight, style, cardWidth)
    canvas.withTranslation(SHOT_CARD_X + SHOT_CARD_PADDING.toFloat(), SHOT_CARD_PADDING.toFloat()) {
        block.header.draw(canvas)
        canvas.translate(0f, block.header.height.toFloat())
        if (block.mediaHeight > 0) {
            canvas.translate(0f, SHOT_SECTION_GAP.toFloat())
            drawShotMedia(
                context,
                loader,
                canvas,
                block.post.attachments,
                style,
                shotMediaSize(cardWidth - SHOT_CARD_PADDING * 2),
            )
            canvas.translate(0f, block.mediaHeight.toFloat())
        }
        block.subject?.let {
            canvas.translate(0f, SHOT_SECTION_GAP.toFloat())
            it.draw(canvas)
            canvas.translate(0f, it.height.toFloat())
        }
        if (block.body.height > 0) {
            canvas.translate(0f, SHOT_SECTION_GAP.toFloat())
            block.body.draw(canvas)
        }
    }
}

private fun drawShotCard(
    canvas: Canvas,
    height: Int,
    style: ThreadShotStyle,
    cardWidth: Int,
) {
    val bounds = RectF(SHOT_CARD_X, 0f, SHOT_CARD_X + cardWidth, height.toFloat())
    canvas.drawRoundRect(bounds, SHOT_CARD_RADIUS, SHOT_CARD_RADIUS, Paint().apply { color = style.surface })
    canvas.drawRoundRect(
        bounds,
        SHOT_CARD_RADIUS,
        SHOT_CARD_RADIUS,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.border
            this.style = Paint.Style.STROKE
            strokeWidth = SHOT_BORDER_WIDTH
        },
    )
}

private suspend fun prepareShotBlocks(
    posts: List<ThreadExportPost>,
    style: ThreadShotStyle,
    cardWidth: Int,
    fontScale: Float,
): List<ThreadShotBlock> {
    val blocks = mutableListOf<ThreadShotBlock>()
    var height = SHOT_CARD_GAP.toLong()
    for (post in posts) {
        coroutineContext.ensureActive()
        val block = prepareShotBlock(post, style, cardWidth, fontScale)
        height += block.height
        validateShotSize(height, cardWidth + SHOT_OUTER_WIDTH)
        blocks += block
    }
    return blocks
}

private fun validateShotSize(
    height: Long,
    width: Int,
) {
    if (height <= 0 || height * width > MAX_SHOT_PIXELS) {
        throw IOException("Слишком большой скриншот: выберите меньше постов")
    }
}

private fun writeShot(
    bitmap: Bitmap,
    destination: OutputStream,
) {
    if (!bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, destination)) {
        throw IOException("Не удалось записать PNG")
    }
    destination.flush()
}

internal const val SHOT_CARD_PADDING = 16
internal const val SHOT_CARD_GAP = 16
internal const val SHOT_SECTION_GAP = 12
internal const val SHOT_MEDIA_GAP = 12
internal const val SHOT_MEDIA_COLUMNS = 4
internal const val SHOT_SCALE = 2f
private const val MAX_SHOT_PIXELS = 8_000_000
private const val SHOT_HEADER_SIZE = 24f
private const val SHOT_CARD_X = 16f
private const val SHOT_CARD_RADIUS = 28f
private const val SHOT_BORDER_WIDTH = 2f
private const val PNG_QUALITY = 100

private fun shotMediaSize(contentWidth: Int): Int =
    ((contentWidth - SHOT_MEDIA_GAP * (SHOT_MEDIA_COLUMNS - 1)) / SHOT_MEDIA_COLUMNS).coerceAtLeast(1)

private const val DEFAULT_SHOT_WIDTH_DP = 360
private const val SHOT_OUTER_WIDTH = 36
private const val SHOT_HEADER_LINE_HEIGHT = 28f
private const val SHOT_LINE_HEIGHT_EXTRA = 5
