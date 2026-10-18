package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

/** Equal cells with genuinely centered labels and accessible radio selection. */
@Composable
fun UiChoiceSegments(
    options: List<String>,
    selected: Int,
    enabled: Boolean,
    lastOptionDescription: String? = null,
    onSelect: (Int) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEachIndexed { index, label ->
            val active = index == selected
            val editable = index == options.lastIndex && lastOptionDescription != null
            Surface(
                modifier =
                    Modifier
                        .weight(1f)
                        .semantics { if (editable) contentDescription = requireNotNull(lastOptionDescription) }
                        .selectable(active, enabled, Role.RadioButton) { onSelect(index) },
                shape = RoundedCornerShape(8.dp),
                color = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                contentColor =
                    if (active) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(Modifier.heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        textDecoration = if (editable) TextDecoration.Underline else TextDecoration.None,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
