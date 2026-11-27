package com.solthariel.pawchan.copyfiles

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.solthariel.pawchan.catalog.CatalogIconButton
import com.solthariel.pawchan.core.ui.ActionMenuItem
import com.solthariel.pawchan.core.ui.AppAlertDialog
import com.solthariel.pawchan.core.ui.NoticeAction
import com.solthariel.pawchan.core.ui.TransientNotice
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiSpacing
import java.text.DateFormat
import java.util.Date
import com.solthariel.pawchan.R.string as L

@Composable
internal fun CopyFileDialogs(
    actions: CopyFileActions,
    choose: (CopyDocument) -> Unit,
    other: () -> Unit,
    saveOther: () -> Unit = other,
    listExtras: CopyFileListExtras? = null,
) {
    val res = LocalResources.current

    TransientNotice(res.getString(L.ui_saving_file).takeIf { actions.busy && !actions.browsing })
    if (actions.browsing) CopyFileListDialog(actions, choose, other, listExtras)
    actions.saved?.let { document ->
        if (actions.kind == CopyFileKind.THREAD) {
            TransientNotice(
                res.getString(L.ui_zip_saved, document.name),
                eventKey = document,
                onDismiss = actions::closeResult,
            )
        } else {
            SavedCopyFileDialog(document, actions::closeResult, actions::browse)
        }
    }
    TransientNotice(
        actions.error,
        action =
            NoticeAction(res.getString(L.ui_choose_another_file)) {
                actions.clearError()
                saveOther()
            },
        onDismiss = actions::clearError,
    )
}

@Composable
private fun CopyFileListDialog(
    actions: CopyFileActions,
    choose: (CopyDocument) -> Unit,
    other: () -> Unit,
    listExtras: CopyFileListExtras?,
) {
    val res = LocalResources.current

    val documents =
        sortedCopyDocuments((actions.documents + listExtras?.additional.orEmpty()).distinctBy { it.uri })
    val unavailable =
        listExtras?.unavailable.orEmpty().filterNot { missing -> documents.any { it.uri == missing.uri } }
    AppAlertDialog(
        onDismissRequest = actions::closeList,
        title = {
            Text(
                if (actions.kind ==
                    CopyFileKind.THREAD
                ) {
                    res.getString(L.ui_zip_files)
                } else {
                    res.getString(L.ui_saved_copies)
                },
            )
        },
        text = {
            Column {
                CopyFileList(actions, documents, unavailable, choose, listExtras)
                if (actions.kind == CopyFileKind.THREAD) {
                    androidx.compose.foundation.layout
                        .Spacer(Modifier.height(UiSpacing.small))
                    ActionMenuItem(
                        res.getString(L.ui_choose_another_file),
                        {
                            actions.closeList()
                            other()
                        },
                        icon = UiIconKind.DOCUMENT,
                        contentPadding = PaddingValues(0.dp),
                    )
                }
            }
        },
        confirmButton =
            if (actions.kind == CopyFileKind.THREAD) {
                null
            } else {
                {
                    TextButton(onClick = {
                        actions.closeList()
                        other()
                    }) { Text(res.getString(L.ui_choose_another_file)) }
                }
            },
    )
}

@Composable
private fun CopyFileList(
    actions: CopyFileActions,
    documents: List<CopyDocument>,
    unavailable: List<CopyDocument>,
    choose: (CopyDocument) -> Unit,
    listExtras: CopyFileListExtras?,
) {
    val res = LocalResources.current

    Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
        if (actions.busy) Text(res.getString(L.ui_reading_folder))
        if (!actions.busy && documents.isEmpty() && unavailable.isEmpty()) {
            Text(
                if (actions.kind == CopyFileKind.THREAD) {
                    res.getString(L.ui_there_are_no_zip_files)
                } else {
                    res.getString(L.ui_there_are_no_copies_in)
                },
            )
        }
        documents.forEach { document ->
            val open = {
                actions.closeList()
                choose(document)
            }
            val more =
                listExtras?.onMore?.let { action ->
                    {
                        actions.closeList()
                        action(document)
                    }
                }
            if (actions.kind == CopyFileKind.THREAD) {
                ZipDocumentRow(document, onOpen = open, onMore = more)
            } else {
                CopyDocumentRow(document, onOpen = open, onMore = more)
            }
        }
        unavailable.forEach { document ->
            ZipDocumentRow(document, onOpen = {}, onMore = null, available = false)
        }
    }
}

@Composable
internal fun CopyDocumentRow(
    document: CopyDocument,
    onOpen: () -> Unit,
    onMore: (() -> Unit)?,
) {
    val res = LocalResources.current

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { ActionMenuItem(copyDocumentLabel(document), onOpen) }
        if (onMore != null) CatalogIconButton(UiIconKind.MORE, res.getString(L.ui_file_actions), onClick = onMore)
    }
}

@Composable
internal fun SavedCopyFileDialog(
    document: CopyDocument,
    close: () -> Unit,
    browse: (() -> Unit)?,
) {
    val res = LocalResources.current

    val context = LocalContext.current
    var showActions by remember(document) { mutableStateOf(false) }
    TransientNotice(
        res.getString(L.ui_copy_saved_cb58, document.name),
        eventKey = document,
        action =
            NoticeAction(res.getString(L.ui_actions)) { showActions = true },
    )
    if (!showActions) return
    AppAlertDialog(
        onDismissRequest = close,
        title = { Text(res.getString(L.ui_copy_saved)) },
        text = {
            Column {
                Text(document.name)
                if (document.mime == "application/zip") {
                    ActionMenuItem(res.getString(L.ui_open), { openCopyFile(context, document, false) })
                }
                ActionMenuItem(res.getString(L.ui_share), { openCopyFile(context, document, true) })
                if (browse != null) {
                    ActionMenuItem(res.getString(L.ui_show_copies), {
                        close()
                        browse()
                    })
                }
            }
        },
        confirmButton = { TextButton(onClick = close) { Text(res.getString(L.ui_done)) } },
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
    } catch (_: SecurityException) {
        android.widget.Toast
            .makeText(context, "Файл недоступен", android.widget.Toast.LENGTH_SHORT)
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
