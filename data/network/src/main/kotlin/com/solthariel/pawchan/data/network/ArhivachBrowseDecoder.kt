package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.archive.ArchiveMedia
import com.solthariel.pawchan.domain.archive.ArchivePost
import com.solthariel.pawchan.domain.archive.ArchivePostDetails
import com.solthariel.pawchan.domain.archive.ArchiveTag
import com.solthariel.pawchan.domain.archive.ArchiveThread
import com.solthariel.pawchan.domain.archive.ArchiveThreadMetadata
import com.solthariel.pawchan.domain.archive.ArchiveThreadPage
import com.solthariel.pawchan.domain.archive.ArchiveThreadSummary
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

internal fun decodeArchiveTags(response: String): List<ArchiveTag>? =
    runCatching {
        val text = response.trim().removeSuffix(";")
        if (!text.startsWith("$JSONP_CALLBACK(") || !text.endsWith(")")) return null
        val json = Json.parseToJsonElement(text.removePrefix("$JSONP_CALLBACK(").dropLast(1)).jsonObject
        if (json["ok"]?.jsonPrimitive?.contentOrNull != "1") return null
        json
            .getValue("tags")
            .jsonArray
            .mapNotNull { item ->
                val tag = item.jsonObject
                val id = tag["id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                val title = tag["title"]?.jsonPrimitive?.contentOrNull?.trim()
                if (id != null && id > 0 && !title.isNullOrEmpty()) ArchiveTag(id, title) else null
            }.distinctBy { it.id }
    }.getOrNull()

internal fun decodeArchiveThreadList(
    html: String,
    url: String,
    nextOffset: Int,
): ArchiveThreadPage? {
    val document = Jsoup.parse(html, url)
    val list = document.selectFirst(".thread_list") ?: return null
    val rows = list.select("tr[id^=thread_row_]")
    val threads = rows.mapNotNull(::archiveThreadSummary)
    val valid = threads.size == rows.size && threads.map { it.id }.distinct().size == threads.size
    val next = document.select("a[href]").any { it.archiveOffset(url) == nextOffset }
    val currentPage = nextOffset / ARCHIVE_PAGE_SIZE
    val lastOffset =
        document
            .selectFirst(".pagination a[title=Последняя страница]")
            ?.archiveOffset(url)
    val totalPages =
        lastOffset
            ?.div(ARCHIVE_PAGE_SIZE)
            ?.plus(1)
            ?.takeIf { it >= currentPage }
            ?: currentPage.takeUnless { next }
    return if (valid) ArchiveThreadPage(threads, next, totalPages) else null
}

internal const val ARCHIVE_PAGE_SIZE = 25

private fun archiveThreadSummary(row: Element): ArchiveThreadSummary? {
    val id =
        row
            .id()
            .removePrefix("thread_row_")
            .toLongOrNull()
            ?.takeIf { it > 0 }
    val link = row.selectFirst(".thread_text a[href]")
    val path = link?.absUrl("href")?.toHttpUrlOrNull()
    val count = row.selectFirst(".thread_posts_count")?.text()?.toIntOrNull()
    val correctHost = path?.scheme == "https" && path.host == "arhivach.vc"
    val correctPath = path?.encodedPath?.trimEnd('/') == "/thread/$id"
    val correctIdentity = correctHost && correctPath
    val missingData = id == null || link == null || count == null
    return if (missingData || !correctIdentity) {
        null
    } else {
        val titleElement = requireNotNull(link).selectFirst("b, strong")
        val linkText = requireNotNull(link).text().trim()
        val title = titleElement?.text()?.trim()?.takeIf(String::isNotEmpty) ?: linkText
        val preview =
            if (titleElement == null) {
                ""
            } else {
                linkText
                    .removePrefix(title)
                    .trim()
                    .removePrefix("—")
                    .removePrefix("-")
                    .trim()
            }
        val thumb = row.selectFirst(".thread_images img[src]")?.absUrl("src")?.safeArchiveMedia()
        val tags = row.select(".thread_tags a[href]").mapNotNull(::archiveTagFromLink).distinctBy { it.id }
        val media = row.select(".thread_images .post_image_block").mapNotNull(::archiveListMedia)
        ArchiveThreadSummary(
            id = requireNotNull(id),
            title = title,
            postCount = requireNotNull(count),
            thumbnailUrl = thumb,
            tags = tags,
            media = media,
            metadata = archiveThreadMetadata(row, preview),
        )
    }
}

private fun archiveThreadMetadata(
    row: Element,
    previewText: String,
): ArchiveThreadMetadata {
    val dateSpan = row.selectFirst(".thread_date span")
    val dateDetails = dateSpan?.attr("title").orEmpty()
    val favoriteControl = row.selectFirst(".thread_control a[id^=infav]")
    return ArchiveThreadMetadata(
        previewText = previewText,
        status = row.selectFirst(".thread_control .label")?.text()?.trim(),
        date = dateSpan?.text()?.trim()?.takeIf(String::isNotEmpty),
        updatedAt = archiveDateDetail(ARCHIVE_UPDATED_PATTERN, dateDetails),
        lastPostAt = archiveDateDetail(ARCHIVE_LAST_POST_PATTERN, dateDetails),
        favorite =
            favoriteControl?.attr("style")?.contains("display:none", ignoreCase = true) == false,
    )
}

private fun archiveDateDetail(
    pattern: Regex,
    dateDetails: String,
): String? =
    pattern
        .find(dateDetails)
        ?.groupValues
        ?.get(1)
        ?.trim()

private fun archiveListMedia(block: Element): ArchiveMedia? {
    val path =
        block
            .selectFirst("a.expand_image[onclick]")
            ?.attr("onclick")
            ?.let { ARCHIVE_LIST_MEDIA_PATTERN.find(it) }
            ?.groupValues
            ?.get(1)
    val original =
        path?.let {
            block
                .baseUri()
                .toHttpUrlOrNull()
                ?.resolve(it)
                ?.toString()
                ?.safeArchiveMedia()
        }
    val thumbnail = block.selectFirst("img[src]")?.absUrl("src")?.safeArchiveMedia()
    return original?.let { ArchiveMedia(it, thumbnail) }
}

private fun archiveTagFromLink(tag: Element): ArchiveTag? {
    val id =
        tag
            .absUrl("href")
            .toHttpUrlOrNull()
            ?.queryParameter("tags")
            ?.toLongOrNull()
    return if (id != null && id > 0 && tag.text().isNotBlank()) ArchiveTag(id, tag.text()) else null
}

internal fun decodeArchiveBrowserThread(
    html: String,
    url: String,
    id: Long,
): ArchiveThread? {
    val doc = Jsoup.parse(html, url).apply { outputSettings().prettyPrint(false) }
    val blocks = doc.select("div.post")
    if (blocks.isEmpty()) return null
    val originalThread = blocks.firstOrNull()?.attr("postid")?.toLongOrNull()
    val posts = blocks.map { archivePost(it, originalThread) }
    return if (posts.any { it == null }) {
        null
    } else {
        ArchiveThread(id, doc.title().removeSuffix(" — Архивач"), posts.filterNotNull())
    }
}

private fun archivePost(
    post: Element,
    originalThread: Long?,
): ArchivePost? {
    val body = post.selectFirst(".post_comment_body") ?: return null
    val media = post.select(".post_image_block").mapNotNull(::archiveMedia)
    val date = post.selectFirst(".post_time")?.text()
    return ArchivePost(
        number = post.attr("postid").takeIf { it.isNotBlank() },
        author = post.selectFirst(".poster_name")?.text(),
        date = date,
        text = body.wholeText().trim(),
        media = media,
        details =
            ArchivePostDetails(
                document = PostDocumentParser.parse(body.html().trim()) { archivePostLink(it, originalThread) },
                subject =
                    post
                        .selectFirst(".post_subject")
                        ?.text()
                        ?.trim()
                        ?.takeIf(String::isNotEmpty),
                timestamp = date?.let(::arhivachTimestamp),
                originalPoster = post.selectFirst(".post_num")?.text()?.trim() == "#1",
                sage = post.archiveSage(),
            ),
    )
}

private fun archiveMedia(block: Element): ArchiveMedia? {
    val original = block.selectFirst("a.img_filename[href]")?.absUrl("href")?.safeArchiveMedia()
    val thumbnail = block.selectFirst("img[src]")?.absUrl("src")?.safeArchiveMedia()
    return original?.let { ArchiveMedia(it, thumbnail, archiveMediaDetails(block)) }
}

private fun String.safeArchiveMedia(): String? {
    val url = toHttpUrlOrNull() ?: return null
    return toString().takeIf {
        url.scheme == "https" && url.host in ARCHIVE_MEDIA_HOSTS && url.port == HTTPS_PORT &&
            url.username.isEmpty() && url.password.isEmpty() && url.encodedPath.startsWith("/storage/")
    }
}

private val ARCHIVE_MEDIA_HOSTS = setOf("arhivach.vc", "i.arhivach.vc")
private val ARCHIVE_LIST_MEDIA_PATTERN = Regex("""^expand_image2\('[^']*','([^']*)',event\); return false;$""")
private val ARCHIVE_UPDATED_PATTERN = Regex("""Обновлен[ао]?:\s*([^\r\n]+)""", RegexOption.IGNORE_CASE)
private val ARCHIVE_LAST_POST_PATTERN = Regex("""Посл\.сообщение:\s*([^\r\n]+)""", RegexOption.IGNORE_CASE)
private const val HTTPS_PORT = 443
