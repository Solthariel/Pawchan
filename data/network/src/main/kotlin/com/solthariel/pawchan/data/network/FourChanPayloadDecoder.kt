package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardDescriptor
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.CatalogSnapshot
import com.solthariel.pawchan.domain.model.CatalogThread
import com.solthariel.pawchan.domain.model.MediaKind
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDetails
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.model.ThreadStatistics
import kotlinx.serialization.json.Json
import org.jsoup.parser.Parser

internal val FOUR_CHAN_SITE = requireNotNull(SiteId.parse("4chan"))

internal class FourChanPayloadDecoder(
    private val json: Json,
) {
    fun boards(payload: String): List<BoardDirectoryEntry>? =
        json
            .decodeFromString<FourChanBoardsDto>(payload)
            .boards
            .mapNotNull { board ->
                val id = board.board.takeIf { it.matches(FOUR_CHAN_BOARD) }?.let(BoardId::parse)
                if (id == null || board.title.isBlank()) {
                    null
                } else {
                    BoardDirectoryEntry(
                        address = BoardAddress(FOUR_CHAN_SITE, id),
                        title = Parser.unescapeEntities(board.title, false),
                        category = if (board.worksafe == 1) "Безопасные" else "Взрослым",
                        postsPerHour = 0,
                        postingEnabled = false,
                    )
                }
            }.takeIf { it.isNotEmpty() }

    fun catalog(
        board: BoardDescriptor,
        payload: String,
    ): CatalogSnapshot? {
        if (board.address.site != FOUR_CHAN_SITE ||
            !board.address.board.value
                .matches(FOUR_CHAN_BOARD)
        ) {
            return null
        }
        val pages = json.decodeFromString<List<FourChanCatalogPageDto>>(payload)
        val threads = pages.flatMap { page -> page.threads.mapNotNull { it.toCatalogThread(board.address) } }
        return CatalogSnapshot(board, threads, boardSpeedAvailable = false)
    }

    fun thread(
        address: ThreadAddress,
        payload: String,
    ): ThreadSnapshot? {
        if (address.site != FOUR_CHAN_SITE ||
            !address.board.board.value
                .matches(FOUR_CHAN_BOARD)
        ) {
            return null
        }
        val posts = json.decodeFromString<FourChanThreadDto>(payload).posts
        val opener = posts.firstOrNull()
        val mapped = posts.mapNotNull { post -> post.toPost(address) }
        val valid =
            opener != null && opener.no == address.thread.value && opener.resto == 0L &&
                mapped.size == posts.size && mapped.map { it.address.post }.toSet().size == mapped.size
        return if (valid) ThreadSnapshot(address, opener.subject(), opener.flags(), mapped) else null
    }
}

private fun FourChanPostDto.toCatalogThread(board: BoardAddress): CatalogThread? {
    val id = ThreadId.parse(no)
    val media = listOfNotNull(attachment(board))
    val fileCount = if (tim != null && filedeleted == 0) 1 else 0
    val files = images.takeIf { it >= 0 && it <= Int.MAX_VALUE - fileCount }?.plus(fileCount)
    val allBytesKnown = media.all { item -> item.byteSize != null }
    val stats =
        files?.let {
            ThreadStatistics.create(
                replies = replies,
                files = it,
                images = media.count { item -> item.kind == MediaKind.IMAGE || item.kind == MediaKind.ANIMATION },
                knownMediaBytes = media.sumOf { item -> item.byteSize?.value ?: 0L },
                mediaBytesComplete = images == 0 && fileCount == media.size && allBytesKnown,
            )
        }
    return if (id != null && resto == 0L && stats != null) {
        CatalogThread(ThreadAddress(board, id), subject(), content(true), media, stats, flags(), time.takeIf { it > 0 })
    } else {
        null
    }
}

private fun FourChanPostDto.toPost(address: ThreadAddress): BoardPost? {
    val id = PostId.parse(no)
    val opener = no == address.thread.value && resto == 0L
    return if (id != null && (opener || resto == address.thread.value)) {
        BoardPost(PostAddress(address, id), content(opener), listOfNotNull(attachment(address.board)))
    } else {
        null
    }
}

private fun FourChanPostDto.subject(): String? {
    val decoded = sub?.let { Parser.unescapeEntities(it, false).trim() }
    return decoded?.takeIf { it.isNotEmpty() }
}

private fun FourChanPostDto.flags(): ThreadFlags = ThreadFlags(sticky != 0, closed != 0, archived != 0)

private fun FourChanPostDto.content(opener: Boolean): PostContent {
    val mail = email?.trim()?.removePrefix("mailto:")?.takeIf { it.isNotBlank() }
    val sage = mail.equals("sage", ignoreCase = true)
    return PostContent(
        document = parseFourChanDocument(com.orEmpty()),
        metadata =
            PostMetadata(
                name?.let { Parser.unescapeEntities(it, false) },
                now,
                sage,
                subject(),
                originalPoster = opener,
            ),
        details =
            PostDetails(
                time.takeIf { it > 0 },
                trip?.let { Parser.unescapeEntities(it, false) },
                mail?.takeUnless { sage },
            ),
    )
}
