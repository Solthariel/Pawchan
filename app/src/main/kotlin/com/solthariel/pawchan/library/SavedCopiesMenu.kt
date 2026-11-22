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

@Composable
internal fun SavedCopiesMenu(
    controller: ArchiveController?,
    count: Int,
    busy: Boolean,
) {
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
        CatalogIconButton(UiIconKind.MORE, "Меню сохранённых копий") { menu = true }
        ModalDropdownMenu(menu, { menu = false }) {
            ActionMenuItem("ZIP-файлы", {
                menu = false
                registry.refresh()
                files.browse()
            }, enabled = files.ready && !files.busy, icon = UiIconKind.ARCHIVE)
            ActionMenuItem("Очистить копии", {
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
    AppAlertDialog(
        onDismissRequest = close,
        title = { Text("Удалить все сохранённые копии?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(UiSpacing.small)) {
                Text(copyDeletionLead(count))
                Text("Загруженные для них медиа будут удалены в фоне.")
                Text("ZIP-файлы и обычные загрузки останутся.")
                Text("Это действие нельзя отменить.")
            }
        },
        confirmButton = { TextButton(onClick = remove) { Text("Удалить") } },
        dismissButton = { TextButton(onClick = close) { Text("Отмена") } },
    )
}

@Composable
private fun ZipFileActions(
    document: CopyDocument,
    close: () -> Unit,
) {
    val context = LocalContext.current
    AppAlertDialog(
        onDismissRequest = close,
        title = { Text(document.name) },
        text = {
            Column {
                ActionMenuItem("Открыть файл", {
                    close()
                    openCopyFile(context, document, false)
                }, icon = UiIconKind.DOCUMENT, contentPadding = PaddingValues(0.dp))
                ActionMenuItem("Поделиться", {
                    close()
                    openCopyFile(context, document, true)
                }, icon = UiIconKind.SHARE, contentPadding = PaddingValues(0.dp))
            }
        },
        confirmButton = { TextButton(onClick = close) { Text("Закрыть") } },
    )
}
