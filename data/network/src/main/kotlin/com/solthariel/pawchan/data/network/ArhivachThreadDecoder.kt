package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDetails
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.time.LocalDateTime
import java.time.ZoneId

/** Fail closed on unknown markup/identity. Current live-site compatibility needs device verification. */
internal fun decodeArhivachThread(
    address: ThreadAddress,
    url: String,
    html: String,
): ThreadSnapshot? {
    val page = Jsoup.parse(html, url)
    val sources =
        page.select("a[href]").mapNotNull { source ->
            source
                .absUrl("href")
                .toHttpUrlOrNull()
                ?.takeIf { it.host in DVACH_SOURCE_HOSTS && "/res/" in it.encodedPath }
        }
    val expectedPath = "/${address.board.board.value}/res/${address.thread.value}.html"
    // Ссылки на исходный тред на странице может не быть; тогда тред узнаётся по номеру ОП-поста.
    if (sources.isNotEmpty() && sources.none { it.encodedPath == expectedPath }) return null
    val blocks = page.select(".post, .post_block")
    val posts = blocks.mapNotNull { decodeArchivePost(address, it) }
    val valid =
        posts.isNotEmpty() && posts.size == blocks.size &&
            posts.map { it.address.post }.distinct().size == posts.size &&
            posts
                .first()
                .address.post.value == address.thread.value
    return if (valid) {
        ThreadSnapshot(
            address,
            page.selectFirst("title")?.text(),
            ThreadFlags(pinned = false, closed = true, archived = true),
            posts.sortedBy { it.address.post.value },
        )
    } else {
        null
    }
}

private fun decodeArchivePost(
    address: ThreadAddress,
    element: Element,
): BoardPost? {
    val number =
        element.attr("data-num").toLongOrNull()
            ?: element
                .id()
                .removePrefix("post_")
                .removePrefix("post")
                .toLongOrNull()
            ?: element
                .selectFirst(".post_num, .post_uid")
                ?.text()
                ?.removePrefix("№")
                ?.trim()
                ?.toLongOrNull()
    val id = number?.let(PostId::parse)
    // Текст поста — только .post_comment_body: в .post_comment рядом лежат блоки картинок и переносы строк.
    val body =
        element.selectFirst(".post_comment_body")
            ?: element.selectFirst(".post_comment, .post_body, .post_message")
    val files = decodeArchiveAttachments(element)
    if (id == null || body == null || files == null) return null
    val date = element.selectFirst(".post_time, .post_date")?.text()?.trim()
    return BoardPost(
        PostAddress(address, id),
        PostContent(
            PostDocumentParser.parse(body.html().trim()),
            PostMetadata(
                name = element.selectFirst(".poster_name, .post_name")?.text()?.trim(),
                date = date,
                sage = element.archiveSage(),
                subject = element.archiveSubject(),
                originalPoster = id.value == address.thread.value,
            ),
            details = PostDetails(timestamp = date?.let(::arhivachTimestamp)),
        ),
        files,
    )
}

private fun Element.archiveSage(): Boolean {
    val mail = selectFirst(".post_mail")?.attr("title").orEmpty()
    return selectFirst(".poster_sage") != null || mail.contains("sage", ignoreCase = true)
}

private fun Element.archiveSubject(): String? {
    val subject = selectFirst(".post_subject")?.text().orEmpty()
    return subject.trim().takeIf(String::isNotEmpty)
}

/** «10/10/26 Суб 20:52:16» — московское время, как в поле date у 2ch. */
internal fun arhivachTimestamp(date: String): Long? {
    val parts =
        ARHIVACH_DATE
            .find(date)
            ?.groupValues
            ?.drop(1)
            ?.map(String::toInt) ?: return null
    val (day, month, year) = parts
    val (hour, minute, second) = parts.drop(DATE_PARTS)
    return runCatching {
        LocalDateTime
            .of(CENTURY + year, month, day, hour, minute, second)
            .atZone(MOSCOW)
            .toEpochSecond()
    }.getOrNull()
}

private val ARHIVACH_DATE = Regex("""(\d{2})/(\d{2})/(\d{2})\D+(\d{2}):(\d{2}):(\d{2})""")
private val MOSCOW: ZoneId = ZoneId.of("Europe/Moscow")
private const val CENTURY = 2000
private const val DATE_PARTS = 3

private val DVACH_SOURCE_HOSTS = setOf("2ch.su", "2ch.hk", "2ch.life", "2ch.pm")
