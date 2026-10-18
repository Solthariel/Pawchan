package com.solthariel.pawchan.thread

import androidx.compose.runtime.staticCompositionLocalOf
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.MediaSaveQueue

interface PostLinkResolver {
    fun postUrl(
        address: ThreadAddress,
        postId: Long,
    ): String?

    fun resolve(
        link: PostLink,
        context: ThreadAddress?,
    ): PostLinkTarget?
}

/** No controller/repository lookup: immutable, route-scoped UI callbacks/resources only. */
data class PostLinkBindings(
    val resolver: PostLinkResolver,
    val transfer: PostMenuTransfer,
    val saves: MediaSaveQueue? = null,
    /** Открыть тред; второй аргумент — пост, к которому прокрутить (null — позиция чтения). */
    val onOpenThread: ((ThreadAddress, Long?) -> Unit)? = null,
    val onOpenBoard: ((BoardAddress) -> Unit)? = null,
    /** «Открыть пост» для прямой ссылки на файл: тред и адрес файла, пост ищется по файлу. */
    val onOpenFilePost: ((ThreadAddress, String) -> Unit)? = null,
)

/** Открытие файла сайта из ссылки во встроенном просмотрщике текущего экрана. */
internal val LocalPostLinkMedia = staticCompositionLocalOf<((MediaAttachmentUi) -> Unit)?> { null }

internal val LocalPostLinks = staticCompositionLocalOf<PostLinkBindings?> { null }
