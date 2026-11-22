package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Shared post surface and spacing; source-specific gestures stay with the caller. */
@Composable
fun PostCardSurface(
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier.fillMaxWidth().padding(8.dp),
    color: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().then(modifier),
        shape = postContainerShape(),
        color = color,
    ) {
        Column(contentModifier, verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
}
