package com.solthariel.pawchan.thread

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.RemoteUrl
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.media.MediaAttachmentUi

/** Что делает короткое нажатие на внешнюю ссылку в тексте поста. */
internal sealed interface PostLinkTap {
    /** Пост: превью, как у «>>»; для другого треда в превью есть «Открыть тред». */
    data class Preview(
        val reference: PostLink.Reference,
    ) : PostLinkTap

    data class Thread(
        val address: ThreadAddress,
        val postId: Long?,
    ) : PostLinkTap

    data class Board(
        val board: BoardAddress,
    ) : PostLinkTap

    data class Media(
        val attachment: MediaAttachmentUi,
    ) : PostLinkTap

    data object Browser : PostLinkTap
}

internal data class PostLinkTapAbilities(
    val preview: Boolean,
    val thread: Boolean,
    val board: Boolean,
    val media: Boolean,
)

internal fun postLinkTap(
    target: PostLinkTarget?,
    abilities: PostLinkTapAbilities,
): PostLinkTap {
    val thread = target?.thread
    val post = target?.postId?.let(PostId::parse)
    val media = target?.viewable
    val board = target?.board
    return when {
        media != null && abilities.media -> PostLinkTap.Media(media)
        thread != null && post != null && abilities.preview -> PostLinkTap.Preview(PostLink.Reference(post, thread))
        thread != null && abilities.thread -> PostLinkTap.Thread(thread, post?.value)
        board != null && abilities.board -> PostLinkTap.Board(board)
        else -> PostLinkTap.Browser
    }
}

internal class PostLinkOpener(
    private val resolver: PostLinkResolver?,
    private val context: ThreadAddress?,
    private val router: ((PostLink.Reference) -> Unit)?,
    private val bindings: PostLinkBindings?,
    private val media: ((MediaAttachmentUi) -> Unit)?,
    private val browser: (String) -> Unit,
) {
    fun open(url: String) {
        val target = RemoteUrl.parse(url)?.let { resolver?.resolve(PostLink.External(it), context) }
        val abilities =
            PostLinkTapAbilities(
                preview = router != null,
                thread = bindings?.onOpenThread != null,
                board = bindings?.onOpenBoard != null,
                media = media != null,
            )
        when (val tap = postLinkTap(target, abilities)) {
            is PostLinkTap.Preview -> router?.invoke(tap.reference)
            is PostLinkTap.Thread -> bindings?.onOpenThread?.invoke(tap.address, tap.postId)
            is PostLinkTap.Board -> bindings?.onOpenBoard?.invoke(tap.board)
            is PostLinkTap.Media -> media?.invoke(tap.attachment)
            PostLinkTap.Browser -> browser(url)
        }
    }
}
