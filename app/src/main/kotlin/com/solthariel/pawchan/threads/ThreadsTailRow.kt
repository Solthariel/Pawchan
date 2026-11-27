package com.solthariel.pawchan.threads

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.R.string as L

@Composable
internal fun ThreadsTailRow(
    expanded: Boolean,
    remaining: Int,
    onToggle: () -> Unit,
) {
    val res = LocalResources.current

    Text(
        if (expanded) res.getString(L.ui_collapse) else res.getString(L.ui_expand_remaining, remaining),
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
