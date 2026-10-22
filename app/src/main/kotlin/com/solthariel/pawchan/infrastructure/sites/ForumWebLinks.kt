package com.solthariel.pawchan.infrastructure.sites

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.MediaKind
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import java.net.URI

/** Site protocol, not a declaration that every forum has this content/address model. */
internal interface ForumWebLinks {
    val site: SiteId

    fun threadUrl(
        address: ThreadAddress,
        postId: Long? = null,
    ): String?

    fun threadAddress(uri: URI): ThreadAddress?

    /** Страница или файл сайта по веб-ссылке; null — ссылка не принадлежит сайту. */
    fun webLink(uri: URI): ForumWebLink? = threadAddress(uri)?.let { ForumWebLink.Thread(it, null) }

    /** Устойчивый путь оригинала файла; нормализация архивов принадлежит сайту. */
    fun mediaPath(uri: URI): String? = uri.path?.takeIf { webLink(uri) is ForumWebLink.Media }

    /**
     * Ввод в поле «Доска, номер, ссылка»: ссылка, «b 123», «/b/123», «/b/», «b».
     * Голое число — тред на [currentBoard]. Формат ввода принадлежит адаптеру сайта.
     */
    fun addressInput(
        input: String,
        currentBoard: BoardAddress?,
    ): ForumWebLink? = null
}

internal sealed interface ForumWebLink {
    data class Board(
        val board: BoardAddress,
    ) : ForumWebLink

    data class Thread(
        val thread: ThreadAddress,
        val postId: Long?,
        val replies: Boolean = false,
    ) : ForumWebLink

    /** Файл, загруженный на сайт, который можно показать встроенным просмотрщиком. */
    data class Media(
        val kind: MediaKind,
        /** Тред, в который загружен файл (2ch: /board/src/<тред>/<файл>); номер поста ссылка не знает. */
        val thread: ThreadAddress? = null,
    ) : ForumWebLink
}
