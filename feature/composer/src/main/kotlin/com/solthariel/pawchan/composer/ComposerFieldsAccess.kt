package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind

internal data class ComposerFieldsAccess(
    val count: Int,
    val onOpen: () -> Unit,
)

@Composable
internal fun ComposerFieldsButton(
    access: ComposerFieldsAccess,
    enabled: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    IconButton(
        onClick = access.onOpen,
        enabled = enabled,
        modifier =
            Modifier.size(48.dp).semantics {
                contentDescription = "Дополнительные поля"
                stateDescription = "Заполнено ${access.count}"
            },
    ) {
        BadgedBox(badge = {
            if (access.count > 0) {
                Badge(containerColor = colors.primaryContainer, contentColor = colors.onPrimaryContainer) {
                    Text(access.count.toString())
                }
            }
        }) {
            UiIcon(
                UiIconKind.TUNE,
                if (enabled ||
                    LocalComposerImporting.current
                ) {
                    colors.onSurfaceVariant
                } else {
                    colors.outline
                },
                Modifier.size(24.dp),
            )
        }
    }
}
