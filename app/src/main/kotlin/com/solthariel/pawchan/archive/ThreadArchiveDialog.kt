package com.solthariel.pawchan.archive

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.solthariel.pawchan.core.ui.ActionMenuItem
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.workspace.LocalWorkspace
import com.solthariel.pawchan.core.ui.AppAlertDialog as AlertDialog

/** «Сохранить копию»: только выбор, с файлами или без; диалог закрывается сразу, итог — всплывающим сообщением. */
@Composable
internal fun ThreadArchiveDialog(
    address: ThreadAddress?,
    onClose: () -> Unit,
) {
    if (address == null) return
    val controller = LocalWorkspace.current?.archives ?: return
    val choose = { media: Boolean ->
        controller.save(address, media)
        onClose()
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Сохранить копию") },
        text = {
            Column {
                ActionMenuItem("С медиа", { choose(true) })
                ActionMenuItem("Без медиа", { choose(false) })
            }
        },
        confirmButton = {},
    )
}
