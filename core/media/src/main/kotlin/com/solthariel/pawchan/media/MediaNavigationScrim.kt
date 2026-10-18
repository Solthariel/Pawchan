package com.solthariel.pawchan.media

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity

/** Draw the navigation-bar color over content, including side navigation in landscape. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun MediaNavigationScrim() {
    val density = LocalDensity.current
    val insets = WindowInsets.navigationBarsIgnoringVisibility
    Box(
        Modifier.fillMaxSize().drawBehind {
            val bottom = insets.getBottom(density).toFloat()
            val left = insets.getLeft(density, layoutDirection).toFloat()
            val right = insets.getRight(density, layoutDirection).toFloat()
            if (bottom > 0f) {
                drawRect(MEDIA_CHROME_BACKGROUND, Offset(0f, size.height - bottom), Size(size.width, bottom))
            }
            // Do not paint overlapping corner pixels twice.
            if (left > 0f) drawRect(MEDIA_CHROME_BACKGROUND, Offset.Zero, Size(left, size.height - bottom))
            if (right > 0f) {
                drawRect(MEDIA_CHROME_BACKGROUND, Offset(size.width - right, 0f), Size(right, size.height - bottom))
            }
        },
    )
}
