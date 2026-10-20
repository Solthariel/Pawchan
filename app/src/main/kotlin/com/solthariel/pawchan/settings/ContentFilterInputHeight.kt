package com.solthariel.pawchan.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Use the standard outlined field height; the floating label shares the same field. */
@Composable
internal fun filterInputMinimumHeight(): Dp =
    with(LocalDensity.current) {
        filterInputHeight(
            MaterialTheme.typography.bodyLarge.lineHeight
                .toDp(),
        )
    }

internal fun filterInputHeight(lineHeight: Dp): Dp = maxOf(56.dp, lineHeight + 32.dp)
