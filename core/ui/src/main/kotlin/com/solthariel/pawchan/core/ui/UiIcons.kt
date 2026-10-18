package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.R

@Composable
fun UiIcon(
    kind: UiIconKind,
    color: Color,
    modifier: Modifier,
) {
    when (kind) {
        UiIconKind.WRITE -> {
            Icon(
                painterResource(R.drawable.ic_thread_compose),
                contentDescription = null,
                tint = color,
                modifier = modifier,
            )
        }

        UiIconKind.ADD -> {
            Icon(
                painter = painterResource(R.drawable.ic_toolbar_add),
                contentDescription = null,
                tint = color,
                modifier = modifier,
            )
        }

        UiIconKind.INFINITY -> {
            Icon(
                painter = painterResource(R.drawable.thread_infinity),
                contentDescription = null,
                tint = color,
                modifier = modifier,
            )
        }

        UiIconKind.CLOSE, UiIconKind.SETTINGS -> {
            UiNavigationIcon(kind, color, modifier)
        }

        UiIconKind.SAGE, UiIconKind.PERSON -> {
            UiPostingIcon(kind, color, modifier)
        }

        in PATH_ICON_KINDS -> {
            UiMediaIcon(kind, color, modifier)
        }

        UiIconKind.PIN, UiIconKind.LOCK, UiIconKind.MEDIA_MULTIPLE -> {
            UiStatusIconCanvas(kind, color, modifier)
        }

        else -> {
            CatalogCanvasIcon(kind, color, modifier)
        }
    }
}

@Composable
private fun CatalogCanvasIcon(
    kind: UiIconKind,
    color: Color,
    modifier: Modifier,
) {
    Canvas(modifier = modifier) {
        val strokeWidth =
            if (kind == UiIconKind.PIN || kind == UiIconKind.LOCK) {
                STATUS_STROKE_DP.dp.toPx()
            } else {
                STANDARD_STROKE_DP.dp.toPx()
            }
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        when (kind) {
            UiIconKind.SEARCH -> drawSearchIcon(color, stroke)
            UiIconKind.BACK -> drawBackIcon(color, stroke)
            UiIconKind.UP -> drawChevronIcon(color, stroke, up = true)
            UiIconKind.DOWN -> drawChevronIcon(color, stroke, up = false)
            UiIconKind.REFRESH -> drawRefreshIcon(color, stroke)
            UiIconKind.MORE -> drawMoreIcon(color, stroke.width)
            UiIconKind.MENU -> drawMenuIcon(color, stroke.width)
            UiIconKind.GRID -> drawGridIcon(color, stroke)
            UiIconKind.DOCUMENT -> drawDocumentIcon(color, stroke)
            UiIconKind.BOOKMARK -> drawBookmarkIcon(color, stroke)
            UiIconKind.CLOSE, UiIconKind.SETTINGS -> Unit
            else -> Unit
        }
    }
}

private fun DrawScope.drawChevronIcon(
    color: Color,
    stroke: Stroke,
    up: Boolean,
) {
    val firstY = if (up) CHEVRON_LOW else CHEVRON_HIGH
    val centerY = if (up) CHEVRON_HIGH else CHEVRON_LOW
    drawLine(
        color,
        Offset(size.width * CHEVRON_LEFT, size.height * firstY),
        Offset(size.width * CHEVRON_CENTER, size.height * centerY),
        stroke.width,
        StrokeCap.Round,
    )
    drawLine(
        color,
        Offset(size.width * CHEVRON_CENTER, size.height * centerY),
        Offset(size.width * CHEVRON_RIGHT, size.height * firstY),
        stroke.width,
        StrokeCap.Round,
    )
}

private fun DrawScope.drawBackIcon(
    color: Color,
    stroke: Stroke,
) {
    drawLine(
        color = color,
        start = Offset(size.width * BACK_END_X, size.height * BACK_CENTER_Y),
        end = Offset(size.width * BACK_START_X, size.height * BACK_CENTER_Y),
        strokeWidth = stroke.width,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color,
        start = Offset(size.width * BACK_START_X, size.height * BACK_CENTER_Y),
        end = Offset(size.width * BACK_ARM_X, size.height * BACK_TOP_Y),
        strokeWidth = stroke.width,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color,
        start = Offset(size.width * BACK_START_X, size.height * BACK_CENTER_Y),
        end = Offset(size.width * BACK_ARM_X, size.height * BACK_BOTTOM_Y),
        strokeWidth = stroke.width,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawSearchIcon(
    color: Color,
    stroke: Stroke,
) {
    val center = Offset(size.width * SEARCH_CENTER, size.height * SEARCH_CENTER)
    drawCircle(
        color,
        minOf(size.width, size.height) * SEARCH_RADIUS,
        center,
        style = stroke,
    )
    drawLine(
        color = color,
        start = Offset(size.width * SEARCH_HANDLE_START, size.height * SEARCH_HANDLE_START),
        end = Offset(size.width * SEARCH_HANDLE_END, size.height * SEARCH_HANDLE_END),
        strokeWidth = stroke.width,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawRefreshIcon(
    color: Color,
    stroke: Stroke,
) {
    drawArc(
        color = color,
        startAngle = REFRESH_START_ANGLE,
        sweepAngle = REFRESH_SWEEP_ANGLE,
        useCenter = false,
        topLeft = Offset(size.width * REFRESH_INSET, size.height * REFRESH_INSET),
        size = Size(size.width * REFRESH_SIZE, size.height * REFRESH_SIZE),
        style = stroke,
    )
    val path =
        Path().apply {
            moveTo(size.width * REFRESH_ARROW_START_X, size.height * REFRESH_ARROW_TOP_Y)
            lineTo(size.width * REFRESH_ARROW_END_X, size.height * REFRESH_INSET)
            lineTo(size.width * REFRESH_ARROW_BOTTOM_X, size.height * REFRESH_ARROW_BOTTOM_Y)
        }
    drawPath(path, color, style = stroke)
}

private fun DrawScope.drawMoreIcon(
    color: Color,
    strokeWidth: Float,
) {
    listOf(ICON_TOP, ICON_MIDDLE, ICON_BOTTOM).forEach { fraction ->
        drawCircle(
            color = color,
            radius = strokeWidth * DOT_RADIUS_MULTIPLIER,
            center = Offset(size.width / 2f, size.height * fraction),
        )
    }
}

private fun DrawScope.drawMenuIcon(
    color: Color,
    strokeWidth: Float,
) {
    listOf(ICON_TOP, ICON_MIDDLE, ICON_BOTTOM).forEach { fraction ->
        drawLine(
            color = color,
            start = Offset(size.width * MENU_LINE_START, size.height * fraction),
            end = Offset(size.width * MENU_LINE_END, size.height * fraction),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawGridIcon(
    color: Color,
    stroke: Stroke,
) {
    val cell = size.width * GRID_CELL
    val gap = size.width * GRID_GAP
    val start = size.width * GRID_START
    listOf(
        Offset(start, start),
        Offset(start + cell + gap, start),
        Offset(start, start + cell + gap),
        Offset(start + cell + gap, start + cell + gap),
    ).forEach { topLeft ->
        drawRoundRect(
            color = color,
            topLeft = topLeft,
            size = Size(cell, cell),
            cornerRadius = CornerRadius(stroke.width, stroke.width),
            style = stroke,
        )
    }
}

private fun DrawScope.drawDocumentIcon(
    color: Color,
    stroke: Stroke,
) {
    val path =
        Path().apply {
            moveTo(size.width * DOCUMENT_LEFT, size.height * DOCUMENT_TOP)
            lineTo(size.width * DOCUMENT_FOLD_X, size.height * DOCUMENT_TOP)
            lineTo(size.width * DOCUMENT_RIGHT, size.height * DOCUMENT_FOLD_Y)
            lineTo(size.width * DOCUMENT_RIGHT, size.height * DOCUMENT_BOTTOM)
            lineTo(size.width * DOCUMENT_LEFT, size.height * DOCUMENT_BOTTOM)
            close()
        }
    drawPath(path, color, style = stroke)
    drawLine(
        color,
        Offset(size.width * DOCUMENT_FOLD_X, size.height * DOCUMENT_TOP),
        Offset(size.width * DOCUMENT_FOLD_X, size.height * DOCUMENT_FOLD_Y),
        stroke.width,
    )
    drawLine(
        color,
        Offset(size.width * DOCUMENT_FOLD_X, size.height * DOCUMENT_FOLD_Y),
        Offset(size.width * DOCUMENT_RIGHT, size.height * DOCUMENT_FOLD_Y),
        stroke.width,
    )
    listOf(DOCUMENT_LINE_TOP, DOCUMENT_LINE_BOTTOM).forEach { fraction ->
        drawLine(
            color = color,
            start = Offset(size.width * DOCUMENT_LINE_START, size.height * fraction),
            end = Offset(size.width * DOCUMENT_LINE_END, size.height * fraction),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawBookmarkIcon(
    color: Color,
    stroke: Stroke,
) {
    val path =
        Path().apply {
            moveTo(size.width * BOOKMARK_LEFT, size.height * BOOKMARK_TOP)
            lineTo(size.width * BOOKMARK_RIGHT, size.height * BOOKMARK_TOP)
            lineTo(size.width * BOOKMARK_RIGHT, size.height * BOOKMARK_BOTTOM)
            lineTo(size.width * BOOKMARK_CENTER, size.height * BOOKMARK_NOTCH)
            lineTo(size.width * BOOKMARK_LEFT, size.height * BOOKMARK_BOTTOM)
            close()
        }
    drawPath(path, color, style = stroke)
}

enum class UiIconKind {
    SEARCH,
    WRITE,
    BACK,
    UP,
    DOWN,
    REFRESH,
    SAGE,
    PERSON,
    MORE,
    MENU,
    GRID,
    DOCUMENT,
    BOOKMARK,
    CLOSE,
    SETTINGS,
    PIN,
    LOCK,
    MEDIA_MULTIPLE,
    INFINITY,
    SAVE,
    PLAY,
    PAUSE,
    PREVIOUS,
    NEXT,
    ROTATE,
    VOLUME_ON,
    VOLUME_OFF,
    PIP,
    MAIL,
    BANNED,
    WARNING,
    EXPAND,
    LINK,
    STAR,
    CHECK,
    TUNE,
    PALETTE,
    CHAT,
    IMAGE,
    FILTER,
    GLOBE,
    INFO,
    ADD,
}

private const val STANDARD_STROKE_DP = 2f
private const val STATUS_STROKE_DP = 1.35f
private const val BACK_START_X = 0.25f
private const val BACK_END_X = 0.75f
private const val BACK_CENTER_Y = 0.5f
private const val BACK_ARM_X = 0.45f
private const val BACK_TOP_Y = 0.3f
private const val BACK_BOTTOM_Y = 0.7f
private const val REFRESH_INSET = 0.17f
private const val REFRESH_SIZE = 0.66f
private const val REFRESH_ARROW_START_X = 0.73f
private const val REFRESH_ARROW_TOP_Y = 0.14f
private const val REFRESH_ARROW_END_X = 0.86f
private const val REFRESH_ARROW_BOTTOM_X = 0.82f
private const val REFRESH_ARROW_BOTTOM_Y = 0.3f
private const val SEARCH_CENTER = 0.43f
private const val SEARCH_RADIUS = 0.28f
private const val SEARCH_HANDLE_START = 0.63f
private const val SEARCH_HANDLE_END = 0.86f
private const val REFRESH_START_ANGLE = 35f
private const val REFRESH_SWEEP_ANGLE = 300f
private const val ICON_TOP = 0.25f
private const val ICON_MIDDLE = 0.5f
private const val ICON_BOTTOM = 0.75f
private const val DOT_RADIUS_MULTIPLIER = 0.8f
private const val MENU_LINE_START = 0.16f
private const val MENU_LINE_END = 0.84f
private const val GRID_CELL = 0.3f
private const val GRID_GAP = 0.14f
private const val GRID_START = 0.13f
private const val DOCUMENT_LEFT = 0.25f
private const val DOCUMENT_TOP = 0.1f
private const val DOCUMENT_FOLD_X = 0.62f
private const val DOCUMENT_RIGHT = 0.8f
private const val DOCUMENT_FOLD_Y = 0.3f
private const val DOCUMENT_BOTTOM = 0.9f
private const val DOCUMENT_LINE_TOP = 0.5f
private const val DOCUMENT_LINE_BOTTOM = 0.67f
private const val DOCUMENT_LINE_START = 0.38f
private const val DOCUMENT_LINE_END = 0.68f
private const val BOOKMARK_LEFT = 0.25f
private const val BOOKMARK_RIGHT = 0.75f
private const val BOOKMARK_TOP = 0.12f
private const val BOOKMARK_BOTTOM = 0.9f
private const val BOOKMARK_CENTER = 0.5f
private const val BOOKMARK_NOTCH = 0.72f
private const val CHEVRON_LEFT = 0.28f
private const val CHEVRON_CENTER = 0.5f
private const val CHEVRON_RIGHT = 0.72f
private const val CHEVRON_HIGH = 0.38f
private const val CHEVRON_LOW = 0.62f
