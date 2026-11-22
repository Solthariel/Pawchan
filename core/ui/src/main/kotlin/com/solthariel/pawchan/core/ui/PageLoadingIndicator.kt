package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The refresh visual for an ongoing load, scaled to the space available. */
@Composable
fun PageLoadingIndicator(
    description: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    foregroundColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    val visual = rememberPullRefreshVisual(PullRefreshRelease(strain = 1f, mode = PullRefreshReleaseMode.LOAD))
    Canvas(
        modifier
            .size(size)
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            },
    ) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val diameter = minOf(this.size.width, this.size.height)
        val scale = diameter / 40.dp.toPx()
        drawRefreshDisc(center, diameter / 2f, scale, backgroundColor)
        drawRefreshMorph(center, diameter * LOADING_MORPH_RADIUS_RATIO, visual.rotation.value, foregroundColor)
    }
}

private const val LOADING_MORPH_RADIUS_RATIO = 0.21875f
