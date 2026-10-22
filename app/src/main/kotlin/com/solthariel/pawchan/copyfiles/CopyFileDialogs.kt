package com.solthariel.pawchan.copyfiles

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.solthariel.pawchan.core.ui.ActionMenuItem
import com.solthariel.pawchan.core.ui.AppAlertDialog
import java.text.DateFormat
import java.util.Date

@Composable
internal fun CopyFileDialogs(
    actions: CopyFileActions,
    choose: (CopyDocument) -> Unit,
    other: () -> Unit,
    saveOther: () -> Unit = other,
) {
    if (actions.busy && !actions.browsing) {
        AppAlertDialog(
            onDismissRequest = {},
            title = { Text("Сохранение копии…") },
            text = { Text("Запись файла") },
            confirmButton = {},
        )
    }
    if (actions.browsing) {
        AppAlertDialog(
            onDismissRequest = actions::closeList,
            title = { Text("Сохранённые копии") },
            text = {
                Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    if (actions.busy) Text("Чтение папки…")
                    if (!actions.busy && actions.documents.isEmpty()) Text("В этой папке пока нет копий")
                    actions.documents.forEach { document ->
                        ActionMenuItem(copyDocumentLabel(document), {
                            actions.closeList()
                            choose(document)
                        })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    actions.closeList()
                    other()
                }) { Text("Выбрать другой файл…") }
            },
        )
    }
    actions.saved?.let { document ->
        SavedCopyFileDialog(document, actions::closeResult, actions::browse)
    }
    actions.error?.let { message ->
        AppAlertDialog(
            onDismissRequest = actions::clearError,
            title = { Text("Не удалось открыть папку") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    actions.clearError()
                    saveOther()
                }) { Text("Выбрать другой файл…") }
            },
        )
    }
}

@Composable
internal fun SavedCopyFileDialog(
    document: CopyDocument,
    close: () -> Unit,
    browse: (() -> Unit)?,
) {
    val context = LocalContext.current
    AppAlertDialog(
        onDismissRequest = close,
        title = { Text("Копия сохранена") },
        text = {
            Column {
                Text(document.name)
                if (document.mime == "application/zip") {
                    ActionMenuItem("Открыть", { openCopyFile(context, document, false) })
                }
                ActionMenuItem("Поделиться", { openCopyFile(context, document, true) })
                if (browse != null) {
                    ActionMenuItem("Показать копии", {
                        close()
                        browse()
                    })
                }
            }
        },
        confirmButton = { TextButton(onClick = close) { Text("Готово") } },
    )
}

internal fun openCopyFile(
    context: Context,
    document: CopyDocument,
    share: Boolean,
) {
    val uri = document.uri.toUri()
    val intent =
        if (share) {
            Intent(Intent.ACTION_SEND).setType(document.mime).putExtra(Intent.EXTRA_STREAM, uri)
        } else {
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, document.mime)
        }
    intent.clipData = ClipData.newRawUri(document.name, uri)
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(Intent.createChooser(intent, document.name))
    } catch (_: ActivityNotFoundException) {
        android.widget.Toast
            .makeText(context, "Нет приложения для открытия файла", android.widget.Toast.LENGTH_SHORT)
            .show()
    }
}

private fun copyDocumentLabel(document: CopyDocument): String {
    val date =
        if (document.modified > 0) {
            DateFormat.getDateTimeInstance().format(Date(document.modified))
        } else {
            "Дата неизвестна"
        }
    val size = if (document.bytes >= 0) "${document.bytes / BYTES_PER_KIB} КБ" else "Размер неизвестен"
    return "${document.name}\n$date, $size"
}

private const val BYTES_PER_KIB = 1024
