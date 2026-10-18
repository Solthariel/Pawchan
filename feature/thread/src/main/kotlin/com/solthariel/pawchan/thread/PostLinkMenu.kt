package com.solthariel.pawchan.thread

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.ActionMenuItem
import com.solthariel.pawchan.core.ui.AppDialog
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.media.MediaSaveQueue
import com.solthariel.pawchan.media.rememberMediaSaveActions

internal val LocalPostLinkAddress = staticCompositionLocalOf<ThreadAddress?> { null }

/** Long pressing a link is independent of posting, and never invokes a share/send action. */
@Composable
internal fun PostLinkMenu(
    target: PostLinkTarget?,
    onDismiss: () -> Unit,
) {
    if (target == null) return
    val context = LocalContext.current
    val uri = LocalUriHandler.current
    val bindings = LocalPostLinks.current
    AppDialog(onDismiss, Modifier.testTag("post-link-menu")) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
            LinkMenuRow("Копировать ссылку") {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Ссылка", target.url))
                onDismiss()
            }
            LinkMenuRow("Открыть в браузере") {
                uri.openUri(target.url)
                onDismiss()
            }
            bindings?.saves?.let { queue ->
                target.attachment?.let { LinkDownloadAction(it, queue, onDismiss) }
            }
            bindings?.onOpenFilePost?.let { open ->
                target.fileThread?.let { address ->
                    LinkMenuRow("Открыть пост") {
                        onDismiss()
                        open(address, target.url)
                    }
                }
            }
            bindings?.onOpenThread?.let { navigate ->
                target.thread?.let { address ->
                    LinkMenuRow(target.openLabel(address)) {
                        onDismiss()
                        navigate(address, target.postId)
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkDownloadAction(
    attachment: com.solthariel.pawchan.media.MediaAttachmentUi,
    queue: MediaSaveQueue,
    onDismiss: () -> Unit,
) {
    val save = rememberMediaSaveActions(queue)
    LinkMenuRow("Скачать файл") { save.request(attachment) }
    // Keep the launcher composed until the document picker has returned.
    androidx.compose.runtime.LaunchedEffect(save.status) {
        if (save.status !is com.solthariel.pawchan.media.MediaSaveStatus.Idle) onDismiss()
    }
}

@Composable
private fun LinkMenuRow(
    label: String,
    onClick: () -> Unit,
) {
    ActionMenuItem(label, onClick)
}

/** Ссылка на пост (не ОП) ведёт к посту, на тред или ОП-пост — к треду. */
internal fun PostLinkTarget.openLabel(address: ThreadAddress): String =
    if (postId != null && postId != address.thread.value) "Открыть пост" else "Открыть тред"
