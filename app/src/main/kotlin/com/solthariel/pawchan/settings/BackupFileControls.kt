package com.solthariel.pawchan.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.net.toUri
import com.solthariel.pawchan.backup.ClientBackupActions
import com.solthariel.pawchan.copyfiles.CopyFileDialogs
import com.solthariel.pawchan.copyfiles.CopyFileKind
import com.solthariel.pawchan.copyfiles.SavedCopyFileDialog
import com.solthariel.pawchan.copyfiles.rememberCopyFiles

@Composable
internal fun backupFileControls(actions: ClientBackupActions): () -> Unit {
    val files = rememberCopyFiles(CopyFileKind.BACKUP)
    val save =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/octet-stream"),
            actions::save,
        )
    val load =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) actions.read(uri)
        }
    val other = { load.launch(arrayOf("*/*")) }
    LaunchedEffect(actions.exportReady, files.ready, files.tree) {
        if (actions.exportReady && files.ready) {
            val tree = files.tree
            if (tree == null) {
                save.launch("Pawchan-backup-${System.currentTimeMillis()}.pawbackup")
            } else {
                actions.saveFolder(files.store, tree)
            }
        }
    }
    if (actions.canRetrySave && actions.message != null) {
        TextButton(onClick = { save.launch("Pawchan-backup-${System.currentTimeMillis()}.pawbackup") }) {
            Text("Сохранить в другое место…")
        }
    }
    CopyFileDialogs(files, { actions.read(it.uri.toUri()) }, other)
    actions.saved?.let { document ->
        SavedCopyFileDialog(document, actions::consumeSaved, files::browse.takeIf { files.tree != null })
    }
    return {
        if (files.ready) {
            if (files.tree == null) other() else files.browse()
        }
    }
}
