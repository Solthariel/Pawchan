package com.solthariel.pawchan.core.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource

/** Rounded thumbs keep the existing rating colors and selected/unselected distinction. */
@Composable
fun UiRatingIcon(
    up: Boolean,
    filled: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(ratingIconResource(up, filled)),
        contentDescription = null,
        tint = color,
        modifier = modifier,
    )
}

internal fun ratingIconResource(
    up: Boolean,
    filled: Boolean,
): Int =
    when {
        up && filled -> R.drawable.ic_rounded_thumb_up_fill
        up -> R.drawable.ic_rounded_thumb_up
        filled -> R.drawable.ic_rounded_thumb_down_fill
        else -> R.drawable.ic_rounded_thumb_down
    }

@Composable
@ReadOnlyComposable
fun ratingLikeColor(): Color {
    val dark = MaterialTheme.colorScheme.surface.luminance() < DARK_SURFACE
    return if (dark) Color(LIKE_DARK) else Color(LIKE_LIGHT)
}

@Composable
@ReadOnlyComposable
fun ratingDislikeColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() < DARK_SURFACE) Color(DISLIKE_DARK) else Color(DISLIKE_LIGHT)

private const val DARK_SURFACE = 0.4f
private const val LIKE_LIGHT = 0xFF2E7D32
private const val LIKE_DARK = 0xFF66BB6A
private const val DISLIKE_LIGHT = 0xFFC62828
private const val DISLIKE_DARK = 0xFFEF5350
