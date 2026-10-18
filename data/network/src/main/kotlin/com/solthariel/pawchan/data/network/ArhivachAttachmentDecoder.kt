package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.AttachmentId
import com.solthariel.pawchan.domain.model.AttachmentSource
import com.solthariel.pawchan.domain.model.MediaAttachment
import com.solthariel.pawchan.domain.model.MediaKind
import com.solthariel.pawchan.domain.model.RemoteUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.nodes.Element

internal fun decodeArchiveAttachments(post: Element): List<MediaAttachment>? {
    val links =
        post
            .select(".post_file a[href], .post_files a[href], .post_image a[href]")
            .filter { it.selectFirst("img") != null || it.hasClass("filelink") }
            .distinctBy { it.absUrl("href") }
    val files = links.mapNotNull(::decodeArchiveAttachment)
    return files.takeIf { it.size == links.size }
}

private fun decodeArchiveAttachment(link: Element): MediaAttachment? {
    val raw = link.absUrl("href")
    val url =
        raw.toHttpUrlOrNull()?.takeIf {
            it.scheme == "https" && it.host == "arhivach.vc" && it.encodedPath.startsWith("/storage/")
        } ?: return null
    val name = url.pathSegments.last()
    val kind =
        when (name.substringAfterLast('.').lowercase()) {
            "jpg", "jpeg", "png", "webp", "avif" -> MediaKind.IMAGE
            "gif" -> MediaKind.ANIMATION
            "mp4", "webm", "mkv" -> MediaKind.VIDEO
            "mp3", "ogg", "wav" -> MediaKind.AUDIO
            else -> MediaKind.FILE
        }
    return MediaAttachment(
        requireNotNull(AttachmentId.parse(raw)),
        name,
        AttachmentSource(
            requireNotNull(RemoteUrl.parse(raw)),
            link.selectFirst("img")?.absUrl("src")?.let(RemoteUrl::parse),
        ),
        byteSize = null,
        kind = kind,
    )
}
