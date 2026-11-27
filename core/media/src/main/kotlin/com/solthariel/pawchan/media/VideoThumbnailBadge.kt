package com.solthariel.pawchan.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind

/** A visual marker only. The thumbnail owns the tap, long press, focus and accessibility action. */
@Composable
fun VideoThumbnailBadge(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val diameter = if (compact) 24.dp else 32.dp
    val iconSize = if (compact) 20.dp else 24.dp
    Box(
        modifier.size(diameter).background(Color.Black.copy(alpha = VIDEO_THUMBNAIL_SCRIM_ALPHA), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        UiIcon(UiIconKind.VIDEO_PREVIEW_PLAY, Color.White, Modifier.size(iconSize))
    }
}

internal const val VIDEO_THUMBNAIL_SCRIM_ALPHA = 0.60f
