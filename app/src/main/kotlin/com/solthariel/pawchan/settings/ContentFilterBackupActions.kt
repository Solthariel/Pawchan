package com.solthariel.pawchan.settings

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.core.ui.AppAlertDialog as AlertDialog

internal data class ContentFilterBackupActions(
    val save: () -> Unit,
    val load: () -> Unit,
    val busy: Boolean = false,
    val canRestore: Boolean = true,
)

@Composable
internal fun rememberContentFilterBackup(
    rules: List<ContentFilterRule>,
    restore: (suspend (List<ContentFilterRule>) -> Int?)?,
): ContentFilterBackupActions {
    val resolver = LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    val state = remember(resolver, scope) { ContentFilterBackupState(resolver, scope) }
    state.rules = rules
    state.restore = restore
    val save =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            if (uri != null) state.save(uri)
        }
    val load =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null && restore != null) state.load(uri)
        }
    state.message?.let { text ->
        AlertDialog(
            onDismissRequest = { state.message = null },
            text = { Text(text) },
            confirmButton = { TextButton(onClick = { state.message = null }) { Text("Закрыть") } },
        )
    }
    return ContentFilterBackupActions(
        save = { launchBackupPicker(state) { save.launch("Pawchan_autohide.txt") } },
        load = {
            launchBackupPicker(state) { load.launch(arrayOf("text/plain", "application/octet-stream")) }
        },
        busy = state.busy,
        canRestore = restore != null,
    )
}

private fun launchBackupPicker(
    state: ContentFilterBackupState,
    launch: () -> Unit,
) {
    try {
        launch()
    } catch (_: ActivityNotFoundException) {
        state.message = "Не найдено приложение для выбора файла"
    } catch (_: SecurityException) {
        state.message = "Нет доступа к выбору файла"
    }
}
