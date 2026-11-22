package com.solthariel.pawchan.threads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.R
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind

private val ThreadListStatusUi.hasNoIcons: Boolean
    get() = !pinned && !closed && !endless && additional.isEmpty()

@Composable
internal fun ThreadListStatusIcons(
    status: ThreadListStatusUi,
    modifier: Modifier = Modifier,
    iconSize: Dp = 15.dp,
) {
    if (status.hasNoIcons) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        val colors = MaterialTheme.colorScheme
        if (status.pinned) {
            ThreadListStatusIcon(UiIconKind.PIN, stringResource(R.string.thread_pinned), colors.primary, iconSize)
        }
        if (status.closed) {
            ThreadListStatusIcon(UiIconKind.LOCK, stringResource(R.string.thread_closed_short), colors.error, iconSize)
        }
        if (status.endless) {
            ThreadListStatusIcon(
                UiIconKind.INFINITY,
                stringResource(R.string.thread_endless),
                colors.tertiary,
                iconSize,
            )
        }
        status.additional.forEach { marker ->
            val color =
                when (marker.tone) {
                    ThreadListStatusTone.PRIMARY -> colors.primary
                    ThreadListStatusTone.SECONDARY -> colors.secondary
                    ThreadListStatusTone.TERTIARY -> colors.tertiary
                    ThreadListStatusTone.ERROR -> colors.error
                    ThreadListStatusTone.MUTED -> colors.onSurfaceVariant
                }
            ThreadListStatusIcon(marker.icon, marker.description, color, iconSize)
        }
    }
}

@Composable
private fun ThreadListStatusIcon(
    kind: UiIconKind,
    description: String,
    color: Color,
    iconSize: Dp,
) {
    UiIcon(
        kind = kind,
        color = color,
        modifier = Modifier.size(iconSize).semantics { contentDescription = description },
    )
}
