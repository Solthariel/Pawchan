package com.solthariel.pawchan.threads

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind

/** Display metadata supplied by the app's registered sources, never inferred from a domain. */
internal data class ThreadSourceUi(
    val title: String,
    @param:DrawableRes val icon: Int? = null,
)

/** Non-interactive site mark: 24dp silhouette, 16dp gap, no badge or persistent background. */
@Composable
internal fun ThreadSourceIcon(
    source: ThreadSourceUi,
    current: Boolean,
) {
    val color =
        if (current) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        Modifier.padding(end = 16.dp).size(24.dp).semantics { contentDescription = "Форум: ${source.title}" },
        contentAlignment = Alignment.Center,
    ) {
        val icon = source.icon
        if (icon != null) {
            Icon(painterResource(icon), null, Modifier.size(24.dp), tint = color)
        } else {
            UiIcon(UiIconKind.PUBLIC, color, Modifier.size(24.dp))
        }
    }
}
