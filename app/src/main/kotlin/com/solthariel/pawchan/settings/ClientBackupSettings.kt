package com.solthariel.pawchan.settings

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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.backup.BackupSection
import com.solthariel.pawchan.backup.BackupStores
import com.solthariel.pawchan.backup.ClientBackupActions
import com.solthariel.pawchan.backup.ClientBackupService
import com.solthariel.pawchan.backup.DEFAULT_BACKUP_SECTIONS
import com.solthariel.pawchan.core.ui.TransientNotice
import com.solthariel.pawchan.data.maintenance.MaintenanceGate
import com.solthariel.pawchan.data.preferences.DataStoreSettingsRepository
import com.solthariel.pawchan.workspace.LocalWorkspace
import java.io.File
import com.solthariel.pawchan.R.string as L

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
    val res = LocalResources.current

    var choosing by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(DEFAULT_BACKUP_SECTIONS) }
    val load = backupFileControls(actions)
    LaunchedEffect(actions.preview) { actions.preview?.let { selected = it.backup.sections } }
    BackupActionRows(actions, create = {
        actions.discard()
        selected = DEFAULT_BACKUP_SECTIONS
        choosing = true
    }, load = load)
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
    TransientNotice(
        res.getString(L.ui_backup_operation_in_progress).takeIf { actions.busy && !actions.passwordNeeded },
    )
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
    val res = LocalResources.current

    Column {
        SettingsItemRow(
            SettingsItem.Action(res.getString(L.ui_create_backup), null, onClick = {
                if (!actions.busy && !actions.exportReady) create()
            }),
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        SettingsItemRow(
            SettingsItem.Action(res.getString(L.ui_restore_from_copy), null, onClick = {
                if (!actions.busy && !actions.exportReady) load()
            }),
        )
        Text(
            res.getString(L.ui_no_tabs_drafts_downloaded_files),
            Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodySmall,
        )
        TransientNotice(actions.message.takeUnless { actions.canRetrySave })
    }
}
