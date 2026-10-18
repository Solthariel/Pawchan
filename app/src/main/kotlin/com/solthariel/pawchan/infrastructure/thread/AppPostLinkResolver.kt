package com.solthariel.pawchan.infrastructure.thread

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.infrastructure.sites.ForumWebLink
import com.solthariel.pawchan.infrastructure.sites.ForumWebLinks
import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.MediaAttachmentSource
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.toAttachmentViewer
import com.solthariel.pawchan.thread.PostLinkResolver
import com.solthariel.pawchan.thread.PostLinkTarget
import java.net.URI
import java.net.URISyntaxException

internal class AppPostLinkResolver(
    adapters: List<ForumWebLinks>,
) : PostLinkResolver {
    private val sites =
        adapters.associateBy { it.site }.also { registrations ->
            check(registrations.size == adapters.size) { "Duplicate link SiteId" }
        }

    override fun postUrl(
        address: ThreadAddress,
        postId: Long,
    ): String? = sites[address.board.site]?.threadUrl(address, postId)

    /** Поле «Доска, номер, ссылка»: голое число относится только к сайту текущей доски. */
    fun addressInput(
        input: String,
        currentBoard: BoardAddress?,
    ): ForumWebLink? {
        val current = currentBoard?.let { sites[it.site] }
        return current?.addressInput(input, currentBoard)?.takeIf { it.belongsTo(current) }
            ?: sites.values.firstNotNullOfOrNull { adapter ->
                adapter.addressInput(input, null)?.takeIf { it.belongsTo(adapter) }
            }
    }

    override fun resolve(
        link: PostLink,
        context: ThreadAddress?,
    ): PostLinkTarget? =
        when (link) {
            is PostLink.Reference -> {
                (link.thread ?: context)?.let { address ->
                    postUrl(address, link.post.value)?.let {
                        val postId = link.post.value.takeIf { id -> id != address.thread.value }
                        PostLinkTarget(it, address, postId = postId)
                    }
                }
            }

            is PostLink.External -> {
                externalTarget(link.url.value)
            }
        }

    private fun externalTarget(url: String): PostLinkTarget? {
        val uri =
            parseUri(url)?.takeIf {
                it.scheme == "https" && it.host != null && it.userInfo == null
            } ?: return null
        val link =
            sites.values.firstNotNullOfOrNull { adapter ->
                adapter.webLink(uri)?.takeIf { it.belongsTo(adapter) }
            }
        val fileName = uri.path.orEmpty().substringAfterLast('/')
        val source = MediaAttachmentSource(url, fileName, url, null)
        val attachment =
            when {
                link is ForumWebLink.Media -> {
                    MediaAttachmentUi(source, null, link.kind.toAttachmentViewer(), link.kind)
                }

                fileName.substringAfterLast('.').lowercase() in DOWNLOAD_EXTENSIONS -> {
                    MediaAttachmentUi(source, null, AttachmentViewer.NONE)
                }

                else -> {
                    null
                }
            }
        val thread = link as? ForumWebLink.Thread
        return PostLinkTarget(
            url,
            thread?.thread,
            attachment,
            postId = thread?.postId,
            board = (link as? ForumWebLink.Board)?.board,
            fileThread = (link as? ForumWebLink.Media)?.thread,
        )
    }

    private fun parseUri(url: String): URI? =
        try {
            URI(url)
        } catch (_: URISyntaxException) {
            null
        }
}

private fun ForumWebLink.belongsTo(adapter: ForumWebLinks): Boolean =
    when (this) {
        is ForumWebLink.Thread -> thread.board.site == adapter.site
        is ForumWebLink.Board -> board.site == adapter.site
        is ForumWebLink.Media -> true
    }

private val DOWNLOAD_EXTENSIONS =
    setOf(
        "jpg",
        "jpeg",
        "png",
        "gif",
        "webp",
        "avif",
        "mp4",
        "webm",
        "mkv",
        "mp3",
        "ogg",
        "pdf",
        "zip",
    )
