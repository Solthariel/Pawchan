package com.solthariel.pawchan.archive

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.solthariel.pawchan.copyfiles.CopyDocument
import com.solthariel.pawchan.copyfiles.CopyFileDialogs
import com.solthariel.pawchan.copyfiles.CopyFileKind
import com.solthariel.pawchan.copyfiles.SavedCopyFileDialog
import com.solthariel.pawchan.copyfiles.openCopyFile
import com.solthariel.pawchan.copyfiles.rememberCopyFiles
import com.solthariel.pawchan.domain.model.ThreadAddress

@Composable
internal fun archiveFileExport(
    controller: ArchiveController,
    address: ThreadAddress,
): () -> Unit {
    val context = LocalContext.current
    val files = rememberCopyFiles(CopyFileKind.THREAD)
    var savedExternally by remember { mutableStateOf<CopyDocument?>(null) }
    val name = "thread-${address.board.board.value}-${address.thread.value}-${System.currentTimeMillis()}.zip"
    val external =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            if (uri != null) {
                controller.export(address, onSaved = {
                    savedExternally =
                        CopyDocument(
                            uri.toString(),
                            name,
                            "application/zip",
                            System.currentTimeMillis(),
                            -1,
                        )
                }) { context.contentResolver.openOutputStream(uri) }
            }
        }
    val other = { external.launch(name) }
    val open =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                openCopyFile(context, CopyDocument(uri.toString(), "Архив треда", "application/zip", 0, -1), false)
            }
        }
    CopyFileDialogs(files, { openCopyFile(context, it, false) }, { open.launch(arrayOf("application/zip")) }, other)
    savedExternally?.let { SavedCopyFileDialog(it, { savedExternally = null }, null) }
    return {
        if (files.ready) {
            if (files.tree == null) {
                other()
            } else {
                files.save(name) { controller.writeZip(address, it) }
            }
        }
    }
}
