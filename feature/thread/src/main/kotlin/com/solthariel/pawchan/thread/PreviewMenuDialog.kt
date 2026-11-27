package com.solthariel.pawchan.thread

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import com.solthariel.pawchan.core.ui.ActionMenuItem
import com.solthariel.pawchan.core.ui.AppMenuDialog
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.feature.thread.R.string as L

@Composable
internal fun PreviewMenuDialog(
    canReply: Boolean,
    onCommand: (PostMenuCommand) -> Unit,
    onClose: () -> Unit,
    hasLink: Boolean = false,
) {
    val res = LocalResources.current

    AppMenuDialog(onClose) {
        ActionMenuItem(
            res.getString(L.ui_reply),
            { onCommand(PostMenuCommand.REPLY) },
            canReply,
            icon = UiIconKind.REPLY,
        )
        ActionMenuItem(
            res.getString(L.ui_quote),
            { onCommand(PostMenuCommand.QUOTE) },
            canReply,
            icon = UiIconKind.QUOTE,
        )
        ActionMenuItem(
            res.getString(L.ui_copy_text),
            { onCommand(PostMenuCommand.COPY_TEXT) },
            icon = UiIconKind.COPY,
        )
        ActionMenuItem(res.getString(L.ui_copy_link), {
            onCommand(PostMenuCommand.COPY_LINK)
        }, hasLink, icon = UiIconKind.LINK)
    }
}
