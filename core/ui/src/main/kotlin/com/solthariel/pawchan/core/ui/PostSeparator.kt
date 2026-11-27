package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * A divider belongs between list items, never inside a post or at a preview's outer edges.
 * It fills its whole-pixel box instead of drawing a 1dp stroke: on fractional densities a stroke leaves a
 * half-covered pixel row, which the screen and the threadshot round differently.
 */
@Composable
fun PostSeparator(
    index: Int,
    lastIndex: Int,
) {
    if (index < lastIndex) {
        Box(
            Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
                .testTag("post-list-divider"),
        )
    }
}
