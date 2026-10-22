package com.solthariel.pawchan.infrastructure.sites

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.MediaKind
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import java.net.URI
import java.net.URISyntaxException

internal class DvachWebLinks(
    private val mirror: String = "2ch.su",
) : ForumWebLinks {
    override val site: SiteId = requireNotNull(SiteId.parse("dvach"))

    override fun threadUrl(
        address: ThreadAddress,
        postId: Long?,
    ): String? {
        if (address.board.site != site || (postId != null && PostId.parse(postId) == null)) return null
        val thread = "https://$mirror/${address.board.board.value}/res/${address.thread.value}.html"
        return if (postId == null) thread else "$thread#$postId"
    }

    override fun threadAddress(uri: URI): ThreadAddress? = (webLink(uri) as? ForumWebLink.Thread)?.thread

    override fun webLink(uri: URI): ForumWebLink? {
        if (!uri.supportedDvachOrigin(mirror)) return null
        val parts = uri.path.orEmpty().split('/')
        val mediaParts = dvachMediaPath(uri)?.split('/') ?: parts
        return threadLink(parts, uri.fragment) ?: mediaLink(mediaParts) ?: boardLink(parts)
    }

    override fun addressInput(
        input: String,
        currentBoard: BoardAddress?,
    ): ForumWebLink? {
        val text = input.trim()
        val url = urlInput(text, mirror)
        val threadInput = THREAD_INPUT.matchEntire(text)?.destructured
        return when {
            text.isEmpty() -> {
                null
            }

            url != null -> {
                webLink(url)
            }

            threadInput != null -> {
                val (board, number, post) = threadInput
                thread(board, number, post.ifEmpty { null })
            }

            // Голое число — номер треда на текущей доске, а не доска с цифровым именем.
            text.all(Char::isDigit) -> {
                currentBoardThread(text, currentBoard)
            }

            else -> {
                BOARD_INPUT.matchEntire(text)?.let { board(it.groupValues[1]) }?.let(ForumWebLink::Board)
            }
        }
    }

    private fun currentBoardThread(
        text: String,
        currentBoard: BoardAddress?,
    ): ForumWebLink? {
        val number = text.toLongOrNull()?.let(ThreadId::parse)
        return if (number != null && currentBoard?.site == site) {
            ForumWebLink.Thread(ThreadAddress(currentBoard, number), null)
        } else {
            null
        }
    }

    override fun mediaPath(uri: URI): String? =
        if (uri.supportedDvachOrigin(mirror) && webLink(uri) is ForumWebLink.Media) dvachMediaPath(uri) else null

    private fun threadLink(
        parts: List<String>,
        fragment: String?,
    ): ForumWebLink? {
        val page = parts.size == PATH_PARTS && parts[RESOURCE_INDEX] == "res" && parts[FILE_INDEX].endsWith(".html")
        val post =
            fragment
                ?.removePrefix("post")
                ?.removePrefix("p")
                ?.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
        return if (page) thread(parts[BOARD_INDEX], parts[FILE_INDEX].removeSuffix(".html"), post) else null
    }

    private fun thread(
        board: String,
        thread: String,
        post: String?,
    ): ForumWebLink? {
        val boardAddress = board(board)
        val threadId = thread.toLongOrNull()?.let(ThreadId::parse)
        val postId = post?.toLongOrNull()?.takeIf { PostId.parse(it) != null && it != threadId?.value }
        return if (boardAddress != null && threadId != null) {
            ForumWebLink.Thread(ThreadAddress(boardAddress, threadId), postId)
        } else {
            null
        }
    }

    private fun mediaLink(parts: List<String>): ForumWebLink? {
        if (parts.size != MEDIA_PATH_PARTS || parts[RESOURCE_INDEX] != "src") return null
        val board = board(parts[BOARD_INDEX])
        val kind = MEDIA_EXTENSIONS[parts.last().substringAfterLast('.', "").lowercase()]
        val thread = parts.getOrNull(FILE_INDEX)?.toLongOrNull()?.let(ThreadId::parse)
        return if (board != null && kind != null) {
            ForumWebLink.Media(kind, thread?.let { ThreadAddress(board, it) })
        } else {
            null
        }
    }

    private fun boardLink(parts: List<String>): ForumWebLink? {
        val page = parts.getOrNull(RESOURCE_INDEX)
        val boardPage =
            when (parts.size) {
                BOARD_ONLY_PARTS -> true
                BOARD_PATH_PARTS -> page.isNullOrEmpty() || page in BOARD_PAGES || BOARD_PAGE.matches(page)
                else -> false
            }
        return if (boardPage) board(parts[BOARD_INDEX])?.let(ForumWebLink::Board) else null
    }

    private fun board(raw: String): BoardAddress? =
        raw
            .lowercase()
            .takeIf { BOARD_NAME.matches(it) && it !in SERVICE_PATHS }
            ?.let(BoardId::parse)
            ?.let { BoardAddress(site, it) }
}

private fun URI.supportedDvachOrigin(mirror: String): Boolean {
    if (!scheme.equals("https", ignoreCase = true) || userInfo != null) return false
    return (host?.lowercase() in HOSTS || host.equals(mirror, ignoreCase = true)) && port in PORTS
}

private fun urlInput(
    text: String,
    mirror: String,
): URI? {
    val recognized = (HOSTS + mirror).any { text.startsWith("$it/", true) || text.equals(it, true) }
    val withScheme =
        when {
            text.startsWith("https://", ignoreCase = true) || text.startsWith("http://", ignoreCase = true) -> {
                text
            }

            recognized -> {
                "https://$text"
            }

            else -> {
                return null
            }
        }
    return try {
        URI(withScheme.replaceFirst(Regex("^http://", RegexOption.IGNORE_CASE), "https://"))
    } catch (_: URISyntaxException) {
        null
    }
}

/** Зеркала 2ch, которые отвечают как один и тот же сайт. */
private val HOSTS = setOf("2ch.hk", "2ch.life", "2ch.su", "2ch.org")
private val PORTS = setOf(-1, HTTPS_PORT)
private val SERVICE_PATHS = setOf("api", "static", "makaba", "user", "stickers", "images", "banners", "sitemap")
private val BOARD_NAME = Regex("[a-z0-9]{1,16}")
private val BOARD_PAGES = setOf("index.html", "catalog.html")
private val BOARD_PAGE = Regex("\\d+\\.html")
private val THREAD_INPUT = Regex("""/?([A-Za-z0-9]{1,16})(?:/res)?(?:\s+|/)(\d+)(?:\.html)?/?(?:#(?:post|p)?(\d+))?""")
private val BOARD_INPUT = Regex("""/?([A-Za-z0-9]{1,16})/?""")
private val MEDIA_EXTENSIONS =
    mapOf(
        "jpg" to MediaKind.IMAGE,
        "jpeg" to MediaKind.IMAGE,
        "png" to MediaKind.IMAGE,
        "webp" to MediaKind.IMAGE,
        "gif" to MediaKind.ANIMATION,
        "mp4" to MediaKind.VIDEO,
        "webm" to MediaKind.VIDEO,
    )
private const val HTTPS_PORT = 443
private const val BOARD_INDEX = 1
private const val RESOURCE_INDEX = 2
private const val FILE_INDEX = 3
private const val PATH_PARTS = 4
private const val MEDIA_PATH_PARTS = 5
private const val BOARD_ONLY_PARTS = 2
private const val BOARD_PATH_PARTS = 3
