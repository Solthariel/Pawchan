package com.solthariel.pawchan.thread

import androidx.compose.runtime.Composable
import com.solthariel.pawchan.core.ui.ActionMenuItem
import com.solthariel.pawchan.core.ui.AppMenuDialog

@Composable
internal fun PreviewMenuDialog(
    canReply: Boolean,
    onCommand: (PostMenuCommand) -> Unit,
    onClose: () -> Unit,
    hasLink: Boolean = false,
) {
    AppMenuDialog(onClose) {
        ActionMenuItem("Ответить", { onCommand(PostMenuCommand.REPLY) }, canReply)
        ActionMenuItem("Цитировать", { onCommand(PostMenuCommand.QUOTE) }, canReply)
        ActionMenuItem("Копировать текст", { onCommand(PostMenuCommand.COPY_TEXT) })
        ActionMenuItem("Копировать ссылку", { onCommand(PostMenuCommand.COPY_LINK) }, hasLink)
    }
}
