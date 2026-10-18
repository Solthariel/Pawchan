package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Иконка из SVG-путей в сетке 24×24: контур толщиной 2dp и (необязательно) заливка. */
internal class UiPathIcon(
    stroke: String?,
    fill: String? = null,
) {
    val stroke: Path? = stroke?.let(::iconPath)
    val fill: Path? = fill?.let(::iconPath)
}

/**
 * Иконки просмотрщика, статусов поста и настроек в той же сетке и толщине, что и остальные
 * иконки тулбара. Пути — строки, поэтому координаты не превращаются в «магические числа».
 */
@Composable
internal fun UiMediaIcon(
    kind: UiIconKind,
    color: Color,
    modifier: Modifier,
) {
    val icon = PATH_ICONS[kind] ?: return
    Canvas(modifier) {
        val unit = size.minDimension / MEDIA_ICON_GRID
        val stroke = MEDIA_ICON_STROKE.dp.toPx() / unit
        scale(unit, unit, pivot = Offset.Zero) {
            icon.fill?.let {
                drawPath(it, color, style = Fill)
                drawPath(it, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            icon.stroke?.let {
                drawPath(it, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

internal val PATH_ICON_KINDS: Set<UiIconKind>
    get() = PATH_ICONS.keys

private const val SPEAKER = "M4 9.5 L7.5 9.5 L12 5.5 L12 18.5 L7.5 14.5 L4 14.5 Z"
private const val CIRCLE = "M12 4 A8 8 0 1 1 11.99 4 Z"

private val PATH_ICONS: Map<UiIconKind, UiPathIcon> =
    mapOf(
        UiIconKind.SAVE to UiPathIcon("M12 4 L12 15 M7.5 10.5 L12 15 L16.5 10.5 M5 19.5 L19 19.5"),
        UiIconKind.PLAY to UiPathIcon(null, "M8 5.5 L18.5 12 L8 18.5 Z"),
        UiIconKind.PAUSE to UiPathIcon(null, "M7 5.5 L10 5.5 L10 18.5 L7 18.5 Z M14 5.5 L17 5.5 L17 18.5 L14 18.5 Z"),
        UiIconKind.PREVIOUS to UiPathIcon("M6.5 6 L6.5 18", "M18 6 L9.5 12 L18 18 Z"),
        UiIconKind.NEXT to UiPathIcon("M17.5 6 L17.5 18", "M6 6 L14.5 12 L6 18 Z"),
        UiIconKind.ROTATE to
            UiPathIcon(
                "M9.5 5 L14.5 5 L14.5 19 L9.5 19 Z M18 7.5 A6.5 6.5 0 0 1 18 16.5 M16.2 16.6 L18 16.5 L18.4 14.8 " +
                    "M6 16.5 A6.5 6.5 0 0 1 6 7.5 M7.8 7.4 L6 7.5 L5.6 9.2",
            ),
        UiIconKind.VOLUME_ON to UiPathIcon("M15.5 9 A4 4 0 0 1 15.5 15 M18 6.5 A7.5 7.5 0 0 1 18 17.5", SPEAKER),
        UiIconKind.VOLUME_OFF to UiPathIcon("M15.5 9.5 L20.5 14.5 M20.5 9.5 L15.5 14.5", SPEAKER),
        UiIconKind.PIP to UiPathIcon("M3.5 6 L20.5 6 L20.5 18 L3.5 18 Z", "M12.5 12 L17.5 12 L17.5 15 L12.5 15 Z"),
        UiIconKind.MAIL to UiPathIcon("M4 6.5 L20 6.5 L20 17.5 L4 17.5 Z M4.5 7 L12 13 L19.5 7"),
        UiIconKind.BANNED to UiPathIcon("$CIRCLE M6.4 6.4 L17.6 17.6"),
        UiIconKind.WARNING to UiPathIcon("M12 4 L21 19.5 L3 19.5 Z M12 10 L12 14 M12 17 L12 17.1"),
        UiIconKind.EXPAND to
            UiPathIcon("M14 4.5 L19.5 4.5 L19.5 10 M19.5 4.5 L13 11 M10 19.5 L4.5 19.5 L4.5 14 M4.5 19.5 L11 13"),
        UiIconKind.LINK to
            UiPathIcon("M10 14 L14 10 M9 11 L7 13 A3.5 3.5 0 0 0 11 17 L13 15 M15 13 L17 11 A3.5 3.5 0 0 0 13 7 L11 9"),
        UiIconKind.STAR to
            UiPathIcon("M12 3.8 L14.5 9 L20.2 9.6 L15.9 13.4 L17.1 19 L12 16.1 L6.9 19 L8.1 13.4 L3.8 9.6 L9.5 9 Z"),
        UiIconKind.CHECK to UiPathIcon("M5 12.5 L10 17 L19 7.5"),
        UiIconKind.TUNE to
            UiPathIcon(
                "M4 7 L12 7 M16 7 L20 7 M14 5 A2 2 0 1 1 13.99 5 M4 17 L8 17 M12 17 L20 17 M10 15 A2 2 0 1 1 9.99 15",
            ),
        UiIconKind.PALETTE to
            UiPathIcon(
                "M12 4 A8 8 0 1 0 12 20 C13.5 20 13.8 18.6 13 17.6 C12.2 16.6 12.8 15 14.3 15 L16 15 " +
                    "A4 4 0 0 0 20 11 C20 7.1 16.4 4 12 4 Z M8 11 L8 11.1 M10.5 7.8 L10.5 7.9 M14.5 8 L14.5 8.1",
            ),
        UiIconKind.CHAT to
            UiPathIcon(
                "M4.5 5.5 L19.5 5.5 L19.5 15.5 L10 15.5 L6 19 L6 15.5 L4.5 15.5 Z " +
                    "M8 9.5 L16 9.5 M8 12.5 L13 12.5",
            ),
        UiIconKind.IMAGE to
            UiPathIcon(
                "M4 5.5 L20 5.5 L20 18.5 L4 18.5 Z " +
                    "M4.5 16 L9 11.5 L13 15.5 L15.5 13 L19.5 17 M15.5 9 L15.5 9.1",
            ),
        UiIconKind.FILTER to UiPathIcon("M4 5.5 L20 5.5 L14 12.5 L14 18.5 L10 16.5 L10 12.5 Z"),
        UiIconKind.GLOBE to
            UiPathIcon("$CIRCLE M4 12 L20 12 M12 4 C8.5 8 8.5 16 12 20 M12 4 C15.5 8 15.5 16 12 20"),
        UiIconKind.INFO to UiPathIcon("$CIRCLE M12 11 L12 16.5 M12 7.8 L12 7.9"),
    )

private const val MEDIA_ICON_GRID = 24f
private const val MEDIA_ICON_STROKE = 2f

private fun iconPath(data: String) = PathParser().parsePathString(data).toPath()
