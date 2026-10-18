package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.R

/** One overlay for the viewport. The disc enters from the edge without scaling during a pull. */
@Composable
internal fun PullRefreshIndicator(
    gesture: PullRefreshGesture,
    visual: PullRefreshVisual,
    bottomInset: Dp,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val style =
        with(LocalDensity.current) {
            PullRefreshStyle(
                20.dp.toPx(),
                64.dp.toPx(),
                8.75.dp.toPx(),
                2.5.dp.toPx(),
                PullRefreshColors(colors.primary, colors.onPrimary),
                bottomInset.toPx(),
            )
        }
    val descriptionId =
        if (gesture.side == PullRefreshSide.TOP) {
            R.string.core_ui_pull_refresh_top
        } else {
            R.string.core_ui_pull_refresh_bottom
        }
    val description = stringResource(descriptionId)
    Canvas(
        modifier.semantics {
            if (gesture.distance != 0f || gesture.requested) {
                contentDescription = description
                progressBarRangeInfo =
                    if (gesture.requested) {
                        ProgressBarRangeInfo.Indeterminate
                    } else {
                        ProgressBarRangeInfo(gesture.strain.coerceIn(0f, 1f), 0f..1f)
                    }
            }
        },
    ) {
        drawPullRefresh(gesture, visual, style)
    }
}

private fun DrawScope.drawPullRefresh(
    gesture: PullRefreshGesture,
    visual: PullRefreshVisual,
    style: PullRefreshStyle,
) {
    val pulling = gesture.distance != 0f
    val release = gesture.release
    val strain =
        when {
            pulling -> {
                gesture.strain
            }

            visual.appliedRelease != release.generation && release.mode != PullRefreshReleaseMode.FINISH -> {
                release.strain
            }

            else -> {
                visual.strain.value
            }
        }
    val scale =
        when {
            pulling -> 1f
            visual.appliedRelease != release.generation && release.mode == PullRefreshReleaseMode.LOAD -> 1f
            else -> visual.scale.value
        }
    if (strain <= 0f || scale <= 0f) return
    val center = Offset(size.width / 2f, pullRefreshCenterY(strain, size.height, style, gesture.side))
    val radius = style.radius * scale
    drawCircle(Color.Black.copy(alpha = SHADOW_ALPHA), radius, center.copy(y = center.y + scale))
    drawCircle(style.colors.background, radius, center)
    val spinning = !pulling && visual.spinning
    val arc = pullRefreshArc(strain, if (spinning) visual.rotation.value else null)
    val ring = style.ringRadius * scale
    drawArc(
        color = style.colors.foreground,
        startAngle = arc.first + if (spinning) visual.spinStartAngle else 0f,
        sweepAngle = arc.second,
        useCenter = false,
        topLeft = Offset(center.x - ring, center.y - ring),
        size = Size(ring * 2f, ring * 2f),
        style = Stroke(style.strokeWidth * scale, cap = StrokeCap.Square),
    )
}

private const val SHADOW_ALPHA = 0.16f
