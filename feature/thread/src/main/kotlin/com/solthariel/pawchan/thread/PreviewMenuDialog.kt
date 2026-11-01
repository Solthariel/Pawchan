package com.solthariel.pawchan.thread

import androidx.compose.runtime.Composable
import com.solthariel.pawchan.core.ui.ActionMenuItem
import com.solthariel.pawchan.core.ui.AppMenuDialog
import com.solthariel.pawchan.core.ui.UiIconKind

@Composable
internal fun PreviewMenuDialog(
    canReply: Boolean,
    onCommand: (PostMenuCommand) -> Unit,
    onClose: () -> Unit,
    hasLink: Boolean = false,
) {
    AppMenuDialog(onClose) {
        ActionMenuItem("Ответить", { onCommand(PostMenuCommand.REPLY) }, canReply, icon = UiIconKind.REPLY)
        ActionMenuItem("Цитировать", { onCommand(PostMenuCommand.QUOTE) }, canReply, icon = UiIconKind.QUOTE)
        ActionMenuItem("Копировать текст", { onCommand(PostMenuCommand.COPY_TEXT) }, icon = UiIconKind.COPY)
        ActionMenuItem("Копировать ссылку", { onCommand(PostMenuCommand.COPY_LINK) }, hasLink, icon = UiIconKind.LINK)
    }
}
