package com.solthariel.pawchan.thread

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.MediaAttachmentUi

/**
 * Разобранная ссылка. [thread] + [postId] — пост в треде (null — сам тред), [board] — доска.
 * [attachment] с viewer != NONE открывается встроенным просмотрщиком, иначе только скачивается.
 * [fileThread] — тред, в который загружен файл по прямой ссылке: пост ищется по файлу («Открыть пост»).
 */
data class PostLinkTarget(
    val url: String,
    val thread: ThreadAddress? = null,
    val attachment: MediaAttachmentUi? = null,
    val postId: Long? = null,
    val board: BoardAddress? = null,
    val fileThread: ThreadAddress? = null,
) {
    val viewable: MediaAttachmentUi?
        get() = attachment?.takeIf { it.viewer != AttachmentViewer.NONE }
}
