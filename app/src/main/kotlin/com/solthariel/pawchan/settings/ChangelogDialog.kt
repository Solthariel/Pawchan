package com.solthariel.pawchan.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton

/** A separate scrollable screen, not an unconstrained text block inside an alert. */
@Composable
internal fun ChangelogDialog(
    releases: List<ChangelogRelease>?,
    failed: Boolean,
    onClose: () -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UiToolbarButton(UiIconKind.BACK, "Назад к настройкам", onClick = onClose)
                Text(
                    "Список изменений",
                    Modifier.weight(1f).padding(start = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                )
            }
            if (releases != null) {
                ChangelogList(releases, Modifier.weight(1f))
            } else {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (failed) Text("Не удалось прочитать список изменений") else CircularProgressIndicator()
                }
            }
        }
    }
}
