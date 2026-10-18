package com.solthariel.pawchan.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.backup.BackupSection
import com.solthariel.pawchan.backup.BackupStores
import com.solthariel.pawchan.backup.ClientBackupActions
import com.solthariel.pawchan.backup.ClientBackupService
import com.solthariel.pawchan.backup.DEFAULT_BACKUP_SECTIONS
import com.solthariel.pawchan.core.ui.AppAlertDialog
import com.solthariel.pawchan.data.maintenance.MaintenanceGate
import com.solthariel.pawchan.data.preferences.DataStoreSettingsRepository
import com.solthariel.pawchan.workspace.LocalWorkspace
import java.io.File

@Composable
internal fun ClientBackupSettings() {
    val binding = LocalWorkspace.current ?: return
    val settings = binding.appSettings as? DataStoreSettingsRepository ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val actions =
        remember(binding, settings, scope) {
            val gate = MaintenanceGate(File(context.filesDir, "backup-maintenance"))
            val stores = BackupStores(settings.portable, binding.library, binding.controller.repository)
            ClientBackupActions(
                ClientBackupService(context.contentResolver, gate, stores),
                scope,
                binding.controller::reload,
            )
        }
    DisposableEffect(actions) { onDispose { actions.close() } }
    ClientBackupControls(actions)
}

@Composable
private fun ClientBackupControls(actions: ClientBackupActions) {
    var choosing by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(DEFAULT_BACKUP_SECTIONS) }
    val save =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
            onResult = actions::save,
        )
    val load =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) actions.read(uri)
        }
    LaunchedEffect(actions.exportReady) {
        if (actions.exportReady) save.launch("Pawchan-backup-${System.currentTimeMillis()}.pawbackup")
    }
    LaunchedEffect(actions.preview) { actions.preview?.let { selected = it.backup.sections } }
    BackupActionRows(actions, create = {
        actions.discard()
        selected = DEFAULT_BACKUP_SECTIONS
        choosing = true
    }, load = { load.launch(arrayOf("*/*")) })
    if (choosing) {
        BackupSelectionDialog(false, selected, { selected = it }, { choosing = false }, {
            choosing = false
            password = true
        })
    }
    if (password || actions.passwordNeeded) {
        BackupPasswordDialog(
            creating = password,
            busy = actions.busy,
            error = actions.message,
            close = {
                password = false
                actions.discard()
            },
            confirm = { secret ->
                if (password) {
                    password = false
                    actions.prepare(selected, secret)
                } else {
                    actions.unlock(requireNotNull(secret))
                }
            },
        )
    }
    BackupRestorePreview(actions, selected, { selected = it })
    if (actions.busy && !actions.passwordNeeded) {
        AppAlertDialog(
            onDismissRequest = {},
            title = { Text("Резервная копия") },
            text = { Text("Выполняется операция…") },
        )
    }
}

@Composable
private fun BackupRestorePreview(
    actions: ClientBackupActions,
    selected: Set<BackupSection>,
    change: (Set<BackupSection>) -> Unit,
) {
    val preview = actions.preview ?: return
    if (actions.busy) return
    BackupSelectionDialog(
        restoring = true,
        selected = selected,
        change = change,
        close = actions::discard,
        confirm = { actions.restore(selected) },
        info =
            remember(preview) {
                BackupSelectionInfo(preview.backup.sections, preview.description, preview.counts)
            },
    )
}

@Composable
private fun BackupActionRows(
    actions: ClientBackupActions,
    create: () -> Unit,
    load: () -> Unit,
) {
    Column {
        SettingsItemRow(
            SettingsItem.Action("Создать резервную копию", null, onClick = {
                if (!actions.busy && !actions.exportReady) create()
            }),
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        SettingsItemRow(
            SettingsItem.Action("Восстановить из копии", null, onClick = {
                if (!actions.busy && !actions.exportReady) load()
            }),
        )
        Text(
            "Без вкладок, черновиков, скачанных файлов, авторизации и PIN приложения.",
            Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodySmall,
        )
        actions.message?.let { Text(it, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium) }
    }
}
