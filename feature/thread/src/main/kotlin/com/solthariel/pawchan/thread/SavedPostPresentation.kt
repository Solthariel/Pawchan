package com.solthariel.pawchan.thread

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoading

/** Prepared on the storage background path. Generic thread state stays private to this renderer. */
class SavedPostPresentation private constructor(
    val address: PostAddress,
    private val post: ThreadPostUi,
    private val content: ThreadUiState.Content,
) {
    val attachments: List<MediaAttachmentUi> get() = post.attachments
    val replyLabel: String? get() = content.presentation.replyIndex[post.id]?.label

    /** Текст поста без разметки — для поиска по Избранному. */
    val plainText: String get() = post.document.plainText

    @Composable
    internal fun Card(
        loading: ThumbnailLoading,
        media: ThreadAttachmentActions,
        interactions: SavedPostInteractions,
    ) {
        CompositionLocalProvider(LocalPostPreview provides interactions.onReference) {
            ThreadPostCard(
                post,
                loading,
                media,
                { id ->
                    interactions.onReference(
                        PostLink.Reference(requireNotNull(PostId.parse(id)), address.thread),
                    )
                },
                ThreadPostOptions(
                    replyLabel = replyLabel,
                    interactions =
                        ThreadPostInteractions(
                            onMenu = interactions.onMenu,
                            onReplies = interactions.onReplies,
                        ),
                ),
            )
        }
    }

    @Composable
    internal fun Preview(
        target: PostPreviewTarget,
        bindings: PostPreviewBindings,
        resources: PostPreviewResources,
    ) {
        PostPreviewDialog(target, content, ThreadAppearance(), bindings, resources)
    }

    @Composable
    internal fun Menu(
        actions: PostPreviewActions,
        onReply: () -> Unit,
        onClose: () -> Unit,
    ) {
        PreviewPostMenu(post, content, actions, onReply, onClose)
    }

    companion object {
        fun prepare(snapshot: ThreadSnapshot): Map<Long, SavedPostPresentation> {
            val content = requireNotNull(DomainOutcome.Success(snapshot).toThreadUiState() as? ThreadUiState.Content)
            return content.posts.associate { post ->
                post.id to
                    SavedPostPresentation(
                        PostAddress(snapshot.address, requireNotNull(PostId.parse(post.id))),
                        post,
                        content,
                    )
            }
        }
    }
}

internal data class SavedPostInteractions(
    val onMenu: () -> Unit,
    val onReplies: () -> Unit,
    val onReference: (PostLink.Reference) -> Unit,
)
