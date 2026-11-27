package com.solthariel.pawchan.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.solthariel.pawchan.core.ui.PageLoadingIndicator
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton
import com.solthariel.pawchan.R.string as L
import com.solthariel.pawchan.core.ui.FullScreenModalWindow as Dialog

/** A separate scrollable screen, not an unconstrained text block inside an alert. */
@Composable
internal fun ChangelogDialog(
    releases: List<ChangelogRelease>?,
    failed: Boolean,
    onClose: () -> Unit,
) {
    val res = LocalResources.current

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = com.solthariel.pawchan.core.ui.APP_TOP_BAR_HEIGHT)
                    .padding(horizontal = com.solthariel.pawchan.core.ui.APP_TOOLBAR_HORIZONTAL_INSET),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UiToolbarButton(UiIconKind.BACK, res.getString(L.ui_back_to_settings), onClick = onClose)
                Text(
                    res.getString(L.ui_change_log),
                    Modifier.weight(1f).padding(start = 12.dp),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                )
            }
            if (releases != null) {
                ChangelogList(releases, Modifier.weight(1f))
            } else {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (failed) {
                        Text(res.getString(L.ui_failed_to_read_changelog))
                    } else {
                        PageLoadingIndicator(description = res.getString(L.ui_loading_change_log))
                    }
                }
            }
        }
    }
}
