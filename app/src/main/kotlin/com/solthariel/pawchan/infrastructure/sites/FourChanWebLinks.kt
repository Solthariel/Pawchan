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

/** Link rules for the experimental 4chan adapter; registration is intentionally separate. */
internal class FourChanWebLinks : ForumWebLinks {
    override val site: SiteId = requireNotNull(SiteId.parse("4chan"))

    override fun threadUrl(
        address: ThreadAddress,
        postId: Long?,
    ): String? {
        val validPost = postId == null || PostId.parse(postId) != null
        if (address.board.site != site || !BOARD.matches(address.board.board.value) || !validPost) {
            return null
        }
        val base = "https://boards.4chan.org/${address.board.board.value}/thread/${address.thread.value}"
        return if (postId == null) base else "$base#p$postId"
    }

    override fun threadAddress(uri: URI): ThreadAddress? = (webLink(uri) as? ForumWebLink.Thread)?.thread

    override fun webLink(uri: URI): ForumWebLink? {
        if (!uri.secureFourChanOrigin() || uri.rawPath != uri.path) return null
        return when (uri.host.lowercase()) {
            in BOARD_HOSTS -> boardPage(uri, site)
            MEDIA_HOST -> mediaPage(uri)
            else -> null
        }
    }

    override fun addressInput(
        input: String,
        currentBoard: BoardAddress?,
    ): ForumWebLink? {
        val text = input.trim()
        val url = inputUrl(text)
        val thread = SHORT_THREAD.matchEntire(text)
        return when {
            text.isEmpty() -> {
                null
            }

            url != null -> {
                webLink(url)
            }

            thread != null -> {
                threadLink(
                    site,
                    thread.groupValues[1],
                    thread.groupValues[THREAD_GROUP],
                    thread.groupValues[FRAGMENT_GROUP],
                )
            }

            text.all(Char::isDigit) -> {
                val id = text.toLongOrNull()?.let(ThreadId::parse)
                if (id != null && currentBoard?.site == site && BOARD.matches(currentBoard.board.value)) {
                    ForumWebLink.Thread(ThreadAddress(currentBoard, id), null)
                } else {
                    null
                }
            }

            else -> {
                SHORT_BOARD.matchEntire(text)?.let { board(site, it.groupValues[1]) }?.let(ForumWebLink::Board)
            }
        }
    }
}

private fun URI.secureFourChanOrigin(): Boolean =
    scheme.equals("https", ignoreCase = true) && userInfo == null && port in setOf(-1, HTTPS_PORT) &&
        host?.lowercase() in (BOARD_HOSTS + MEDIA_HOST)

private fun boardPage(
    uri: URI,
    site: SiteId,
): ForumWebLink? {
    val path = uri.path.orEmpty()
    val thread = THREAD_PATH.matchEntire(path)
    val board = BOARD_PATH.matchEntire(path)
    return when {
        thread != null -> {
            threadLink(site, thread.groupValues[1], thread.groupValues[THREAD_GROUP], uri.fragment.orEmpty())
        }

        board != null -> {
            board(site, board.groupValues[1])?.let(ForumWebLink::Board)
        }

        else -> {
            null
        }
    }
}

private fun threadLink(
    site: SiteId,
    code: String,
    number: String,
    fragment: String,
): ForumWebLink.Thread? {
    val address = board(site, code)
    val id = number.toLongOrNull()?.let(ThreadId::parse)
    val anchor =
        fragment
            .removePrefix("p")
            .toLongOrNull()
            ?.let(PostId::parse)
            ?.value
    return if (address != null && id != null) {
        ForumWebLink.Thread(ThreadAddress(address, id), anchor?.takeUnless { it == id.value })
    } else {
        null
    }
}

private fun board(
    site: SiteId,
    raw: String,
): BoardAddress? =
    raw
        .lowercase()
        .takeIf(BOARD::matches)
        ?.let(BoardId::parse)
        ?.let { BoardAddress(site, it) }

private fun mediaPage(uri: URI): ForumWebLink.Media? {
    val match = MEDIA_PATH.matchEntire(uri.path.orEmpty()) ?: return null
    return MEDIA_EXTENSIONS[match.groupValues[MEDIA_EXTENSION_GROUP].lowercase()]?.let(ForumWebLink::Media)
}

private fun inputUrl(text: String): URI? {
    val bare = (BOARD_HOSTS + MEDIA_HOST).any { text.equals(it, true) || text.startsWith("$it/", true) }
    val url =
        when {
            text.startsWith("https://", true) -> text
            text.startsWith("http://", true) -> "https://${text.substring("http://".length)}"
            bare -> "https://$text"
            else -> return null
        }
    return try {
        URI(url)
    } catch (_: URISyntaxException) {
        null
    }
}

private val BOARD_HOSTS = setOf("boards.4chan.org", "boards.4channel.org")
private const val MEDIA_HOST = "i.4cdn.org"
private val BOARD = Regex("[a-z0-9]{1,16}")
private val BOARD_PATH = Regex("/([a-z0-9]{1,16})(?:/(?:catalog|[0-9]+)?/?)?")
private val THREAD_PATH = Regex("/([a-z0-9]{1,16})/thread/([0-9]+)/?")
private val SHORT_BOARD = Regex("/?([A-Za-z0-9]{1,16})/?")
private val SHORT_THREAD = Regex("/?([A-Za-z0-9]{1,16})(?:/thread)?(?:\\s+|/)([0-9]+)/?(?:#(p[0-9]+))?")
private val MEDIA_PATH = Regex("/[a-z0-9]{1,16}/([0-9]+)\\.([A-Za-z0-9]+)")
private val MEDIA_EXTENSIONS =
    mapOf(
        "jpg" to MediaKind.IMAGE,
        "jpeg" to MediaKind.IMAGE,
        "png" to MediaKind.IMAGE,
        "gif" to MediaKind.ANIMATION,
        "webm" to MediaKind.VIDEO,
        "mp4" to MediaKind.VIDEO,
    )

private const val HTTPS_PORT = 443
private const val THREAD_GROUP = 2
private const val FRAGMENT_GROUP = 3
private const val MEDIA_EXTENSION_GROUP = 2
