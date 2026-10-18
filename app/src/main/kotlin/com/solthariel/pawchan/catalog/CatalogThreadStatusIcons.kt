package com.solthariel.pawchan.catalog

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

@Composable
internal fun CatalogThreadStatusIcons(
    pinned: Boolean,
    closed: Boolean,
    endless: Boolean,
    modifier: Modifier = Modifier,
    iconSize: Dp = 15.dp,
) {
    if (!pinned && !closed && !endless) return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val colors = MaterialTheme.colorScheme
        if (pinned) CatalogThreadStatusIcon(UiIconKind.PIN, R.string.thread_pinned, colors.primary, iconSize)
        if (closed) CatalogThreadStatusIcon(UiIconKind.LOCK, R.string.thread_closed_short, colors.error, iconSize)
        if (endless) CatalogThreadStatusIcon(UiIconKind.INFINITY, R.string.thread_endless, colors.tertiary, iconSize)
    }
}

@Composable
private fun CatalogThreadStatusIcon(
    kind: UiIconKind,
    descriptionResource: Int,
    color: Color,
    iconSize: Dp,
) {
    val description = stringResource(descriptionResource)
    UiIcon(
        kind = kind,
        color = color,
        modifier = Modifier.size(iconSize).semantics { contentDescription = description },
    )
}
