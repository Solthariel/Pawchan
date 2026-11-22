package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Equal-width cells always fit in one row. Full labels remain available to accessibility services. */
@Composable
internal fun AttachmentControlRow(
    count: Int,
    modifier: Modifier = Modifier,
    centeredLikeSix: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    require(count > 0)
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val available = maxWidth - 8.dp
        val slot = (available - 10.dp) / PHOTO_CONTROL_COUNT
        val fraction =
            if (centeredLikeSix &&
                maxWidth > 0.dp
            ) {
                ((slot * count + 2.dp * (count - 1) + 8.dp) / maxWidth).coerceIn(0f, 1f)
            } else {
                1f
            }
        Row(
            Modifier.fillMaxWidth(fraction).padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            content = content,
        )
    }
}

private const val PHOTO_CONTROL_COUNT = 6
