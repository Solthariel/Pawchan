package com.solthariel.pawchan.threads

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ThreadsTailRow(
    expanded: Boolean,
    remaining: Int,
    onToggle: () -> Unit,
) {
    Text(
        if (expanded) "Свернуть" else "Развернуть остальные ($remaining)",
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .heightIn(min = 48.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodyMedium,
    )
}
