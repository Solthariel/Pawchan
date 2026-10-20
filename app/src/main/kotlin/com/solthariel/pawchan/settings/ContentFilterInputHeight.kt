package com.solthariel.pawchan.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Reserve one editable line, not the height of a longer placeholder from another search mode. */
@Composable
internal fun filterInputMinimumHeight(): Dp =
    with(LocalDensity.current) {
        filterInputHeight(
            MaterialTheme.typography.bodyLarge.lineHeight
                .toDp(),
        )
    }

internal fun filterInputHeight(lineHeight: Dp): Dp = maxOf(48.dp, lineHeight + 16.dp)
