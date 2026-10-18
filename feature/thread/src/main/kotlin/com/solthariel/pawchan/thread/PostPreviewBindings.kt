package com.solthariel.pawchan.thread

import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.media.MediaAttachmentUi

internal data class PostPreviewBindings(
    val onClose: () -> Unit,
    val onReference: (PostLink.Reference) -> Unit,
    val onReplies: (Long) -> Unit,
    val onOpen: (MediaAttachmentUi) -> Unit,
    val onDownload: (MediaAttachmentUi) -> Unit,
    val onReply: () -> Unit = {},
    /** Только для превью поста из другого треда: кнопка «Открыть тред». */
    val onOpenThread: ((com.solthariel.pawchan.domain.model.ThreadAddress, Long) -> Unit)? = null,
)
