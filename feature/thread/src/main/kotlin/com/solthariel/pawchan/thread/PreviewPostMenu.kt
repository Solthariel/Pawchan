package com.solthariel.pawchan.thread

import androidx.compose.runtime.Composable
import com.solthariel.pawchan.domain.model.AttachmentId
import com.solthariel.pawchan.domain.model.AttachmentSource
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.ByteSize
import com.solthariel.pawchan.domain.model.MediaAttachment
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.RemoteUrl
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.media.MediaAttachmentUi

@Composable
internal fun PreviewPostMenu(
    post: ThreadPostUi,
    content: ThreadUiState.Content,
    actions: PostPreviewActions,
    onReply: () -> Unit,
    onClose: () -> Unit,
) {
    val address = content.address ?: return
    val bindings = LocalPostLinks.current ?: return
    val links = bindings.resolver
    PreviewMenuDialog(!content.closed, { command ->
        executePreviewPostMenu(command, post, content, actions, bindings.transfer, onReply)
        onClose()
    }, onClose, links.postUrl(address, post.id) != null)
}

internal fun ThreadPostUi.savedBoardPost(address: ThreadAddress): BoardPost =
    BoardPost(
        PostAddress(address, requireNotNull(PostId.parse(id))),
        PostContent(
            document,
            PostMetadata(
                author,
                date,
                sage,
                metadata.display.subject,
                badges = metadata.display.badges,
                originalPoster = metadata.display.originalPoster,
            ),
        ),
        attachments.map { it.savedMedia() },
    )

private fun MediaAttachmentUi.savedMedia(): MediaAttachment =
    MediaAttachment(
        requireNotNull(AttachmentId.parse(id)),
        fileName,
        AttachmentSource(requireNotNull(RemoteUrl.parse(originalUrl)), thumbnailUrl?.let(RemoteUrl::parse)),
        byteSize?.let(ByteSize::parse),
        kind,
        dimensions,
    )
