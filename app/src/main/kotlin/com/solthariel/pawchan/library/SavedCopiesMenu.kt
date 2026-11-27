package com.solthariel.pawchan.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.PawchanApplication
import com.solthariel.pawchan.archive.ArchiveController
import com.solthariel.pawchan.catalog.CatalogIconButton
import com.solthariel.pawchan.copyfiles.CopyDocument
import com.solthariel.pawchan.copyfiles.CopyFileDialogs
import com.solthariel.pawchan.copyfiles.CopyFileKind
import com.solthariel.pawchan.copyfiles.CopyFileListExtras
import com.solthariel.pawchan.copyfiles.openCopyFile
import com.solthariel.pawchan.copyfiles.rememberCopyFiles
import com.solthariel.pawchan.core.ui.ActionMenuItem
import com.solthariel.pawchan.core.ui.AppAlertDialog
import com.solthariel.pawchan.core.ui.ModalDropdownMenu
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiSpacing
import com.solthariel.pawchan.R.string as L

@Composable
internal fun SavedCopiesMenu(
    controller: ArchiveController?,
    count: Int,
    busy: Boolean,
) {
    val res = LocalResources.current

    val files = rememberCopyFiles(CopyFileKind.THREAD)
    val context = LocalContext.current
    val registry = (context.applicationContext as PawchanApplication).zipRegistry
    val indexed by registry.files.collectAsStateWithLifecycle()
    var menu by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<CopyDocument?>(null) }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) selected = CopyDocument(uri.toString(), "ZIP-файл", "application/zip", 0, -1)
        }
    val chooseOther = { picker.launch(arrayOf("application/zip", "application/x-zip-compressed")) }
    Box {
        CatalogIconButton(UiIconKind.MORE, res.getString(L.ui_saved_copies_menu)) { menu = true }
        ModalDropdownMenu(menu, { menu = false }) {
            ActionMenuItem(res.getString(L.ui_zip_files), {
                menu = false
                registry.refresh()
                files.browse()
            }, enabled = files.ready && !files.busy, icon = UiIconKind.ARCHIVE)
            ActionMenuItem(res.getString(L.ui_clear_copies), {
                menu = false
                confirm = true
            }, enabled = controller != null && count > 0 && !busy, icon = UiIconKind.DELETE)
        }
    }
    CopyFileDialogs(
        files,
        { openCopyFile(context, it, false) },
        chooseOther,
        listExtras =
            CopyFileListExtras(
                additional = indexed.filter { it.available }.map { it.document },
                unavailable = indexed.filterNot { it.available }.map { it.document },
                onMore = { selected = it },
            ),
    )
    if (confirm) {
        ClearCopiesDialog(count, { confirm = false }) {
            confirm = false
            controller?.clearAll()
        }
    }
    selected?.let { document -> ZipFileActions(document) { selected = null } }
}

@Composable
private fun ClearCopiesDialog(
    count: Int,
    close: () -> Unit,
    remove: () -> Unit,
) {
    val res = LocalResources.current

    AppAlertDialog(
        onDismissRequest = close,
        title = { Text(res.getString(L.ui_delete_all_saved_copies)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(UiSpacing.small)) {
                Text(copyDeletionLead(count))
                Text(res.getString(L.ui_media_uploaded_for_them_will))
                Text(res.getString(L.ui_zip_files_and_regular_uploads))
                Text(res.getString(L.ui_this_action_cannot_be_undone))
            }
        },
        confirmButton = { TextButton(onClick = remove) { Text(res.getString(L.ui_delete)) } },
        dismissButton = { TextButton(onClick = close) { Text(res.getString(L.ui_cancel)) } },
    )
}

@Composable
private fun ZipFileActions(
    document: CopyDocument,
    close: () -> Unit,
) {
    val res = LocalResources.current

    val context = LocalContext.current
    AppAlertDialog(
        onDismissRequest = close,
        title = { Text(document.name) },
        text = {
            Column {
                ActionMenuItem(res.getString(L.ui_open_file), {
                    close()
                    openCopyFile(context, document, false)
                }, icon = UiIconKind.DOCUMENT, contentPadding = PaddingValues(0.dp))
                ActionMenuItem(res.getString(L.ui_share), {
                    close()
                    openCopyFile(context, document, true)
                }, icon = UiIconKind.SHARE, contentPadding = PaddingValues(0.dp))
            }
        },
        confirmButton = { TextButton(onClick = close) { Text(res.getString(L.ui_close)) } },
    )
}
