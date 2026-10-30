package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingsGroupFrame(
    modifier: Modifier = Modifier,
    horizontalInset: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier.padding(horizontal = horizontalInset).fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(content = content)
    }
}

@Composable
internal fun SettingsGroupHeading(title: String) {
    Text(
        title,
        Modifier.padding(start = 28.dp, end = 16.dp, top = 16.dp, bottom = 6.dp),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
    )
}
