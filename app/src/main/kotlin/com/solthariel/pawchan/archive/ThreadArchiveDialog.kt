package com.solthariel.pawchan.archive

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.core.ui.ActionMenuItem
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiSpacing
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.workspace.LocalWorkspace
import com.solthariel.pawchan.R.string as L
import com.solthariel.pawchan.core.ui.AppAlertDialog as AlertDialog

/** Offline copies remain internal; completed saving exposes opening and ZIP export actions. */
@Composable
internal fun ThreadArchiveDialog(
    address: ThreadAddress?,
    onClose: () -> Unit,
) {
    val res = LocalResources.current

    if (address == null) return
    val binding = LocalWorkspace.current
    val controller = binding?.archives ?: return
    val copies = controller.display.collectAsStateWithLifecycle().value
    val exists = copies.any { it.copy.snapshot.address == address }
    val export = archiveFileExport(controller, address)
    val saved = controller.saved.collectAsStateWithLifecycle().value
    val busy = controller.busy.collectAsStateWithLifecycle().value
    var requested by remember(address) { mutableStateOf(false) }
    val completed = requested && saved == address
    val choose = { media: Boolean ->
        controller.consumeSaved()
        requested = true
        controller.save(address, media)
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(res.getString(if (completed) L.ui_copy_saved else L.ui_save_copy)) },
        text = {
            Column {
                if (exists) {
                    ActionMenuItem(res.getString(L.ui_open_saved_copy), {
                        binding.controller.offlineAddress = address
                        binding.navigate(address)
                        onClose()
                    }, icon = UiIconKind.FORUM, contentPadding = PaddingValues(0.dp))
                    ActionMenuItem(
                        res.getString(L.ui_save_zip),
                        export,
                        icon = UiIconKind.ARCHIVE,
                        contentPadding = PaddingValues(0.dp),
                    )
                }
                if (busy) Text(res.getString(L.ui_saving))
                if (!completed) {
                    ActionMenuItem(
                        res.getString(L.ui_with_media),
                        { choose(true) },
                        enabled = !busy,
                        icon = UiIconKind.MEDIA_MULTIPLE,
                        contentPadding = PaddingValues(0.dp),
                    )
                    ActionMenuItem(
                        res.getString(L.ui_no_media),
                        { choose(false) },
                        enabled = !busy,
                        icon = UiIconKind.DOCUMENT,
                        contentPadding = PaddingValues(0.dp),
                    )
                }
                if (exists) ArchiveZipHint()
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun ArchiveZipHint() {
    val res = LocalResources.current

    Text(
        res.getString(L.ui_zip_includes_only_already_uploaded),
        Modifier.padding(top = UiSpacing.small),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
