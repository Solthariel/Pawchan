package com.solthariel.pawchan.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.UiSpacing

@Composable
internal fun SettingsGroupFrame(
    modifier: Modifier = Modifier,
    horizontalInset: Dp = UiSpacing.medium,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .padding(horizontal = horizontalInset)
            .fillMaxWidth()
            .clip(SETTINGS_GROUP_SHAPE)
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        content = content,
    )
}

@Composable
internal fun SettingsGroupHeading(title: String) {
    Text(
        title,
        Modifier.padding(start = 28.dp, end = UiSpacing.large, top = UiSpacing.large, bottom = 6.dp),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
    )
}

private val SETTINGS_GROUP_SHAPE = RoundedCornerShape(24.dp)
