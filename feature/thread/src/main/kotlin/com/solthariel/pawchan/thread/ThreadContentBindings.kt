package com.solthariel.pawchan.thread

import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader

internal data class ThreadContentBindings(
    val loader: ThumbnailLoader,
    val onOpen: (MediaAttachmentUi) -> Unit,
    val onDownload: (MediaAttachmentUi) -> Unit,
    val onMediaMenu: (MediaAttachmentUi) -> Unit,
    val postActions: ThreadContentPostActions,
    val tools: ThreadToolsState,
    val appearance: ThreadAppearance,
)

internal data class ThreadContentPostActions(
    val onQuote: (String) -> Unit,
    val onShowHidden: (ThreadPostUi) -> Unit,
)
