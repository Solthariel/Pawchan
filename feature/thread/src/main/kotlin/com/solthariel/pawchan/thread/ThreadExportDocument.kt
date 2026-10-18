package com.solthariel.pawchan.thread

import androidx.compose.ui.text.AnnotatedString
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.media.MediaAttachmentUi

/** Rendering input only: no navigation, preferences, mutable tools or rich-text cache. */
data class ThreadExportDocument(
    val id: Long,
    val subject: String?,
    val posts: List<ThreadExportPost>,
)

data class ThreadExportPost(
    val index: Int,
    val header: AnnotatedString,
    val subject: String?,
    val document: PostDocument,
    val attachments: List<MediaAttachmentUi>,
)
