package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ActionMenuItem(
    label: String,
    action: () -> Unit,
    enabled: Boolean = true,
    navigation: MenuNavigation? = null,
    icon: UiIconKind? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
) {
    TextButton(
        onClick = action,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        contentPadding = contentPadding,
    ) {
        if (navigation == MenuNavigation.BACK) {
            UiBackwardIndicator()
            Spacer(Modifier.width(4.dp))
        }
        if (icon != null && navigation != MenuNavigation.BACK) {
            UiMenuIcon(icon)
            Spacer(Modifier.width(12.dp))
        }
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        if (navigation == MenuNavigation.FORWARD) UiSubmenuIndicator()
    }
}
