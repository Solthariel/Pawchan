package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind

/** Expanding is independent of requesting. A check opens results and never auto-hides them. */
@Composable
internal fun MirrorAvailabilityResults(
    presentation: MirrorAvailabilityPresentation,
    checking: Boolean,
    onCheck: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).semantics { stateDescription = if (expanded) "Развёрнуто" else "Свёрнуто" }) {
            SettingsRow(
                "Доступность зеркал",
                if (checking) "Проверка доступности…" else presentation.summary ?: "Ещё не проверено",
                onClick = { expanded = !expanded },
            ) {
                UiIcon(
                    if (expanded) UiIconKind.UP else UiIconKind.DOWN,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    Modifier.size(24.dp),
                )
            }
        }
        IconButton(
            onClick = {
                expanded = true
                onCheck()
            },
            enabled = !checking,
            modifier = Modifier.semantics { contentDescription = "Проверить доступность" },
        ) {
            if (checking) {
                CircularProgressIndicator(Modifier.size(24.dp))
            } else {
                UiIcon(UiIconKind.REFRESH, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(24.dp))
            }
        }
    }
    if (expanded) MirrorStatusRows(presentation)
}

@Composable
private fun MirrorStatusRows(presentation: MirrorAvailabilityPresentation) {
    presentation.rows.forEach { row ->
        SettingsRow(
            row.host,
            listOfNotNull(row.caption, row.responseCaption).joinToString(" — "),
            onClick = null,
        )
    }
}
