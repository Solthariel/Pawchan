package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.RemoteUrl
import org.jsoup.nodes.Element

internal fun archivePostLink(
    anchor: Element,
    originalThread: Long?,
): PostLink? {
    val href = anchor.attr("href").trim()
    val number = anchor.attr("data-num").toLongOrNull()?.let(PostId::parse)
    val threadAttribute = anchor.attr("data-thread")
    val targetThread = threadAttribute.toLongOrNull()
    val localThread = threadAttribute.isBlank() || (originalThread != null && targetThread == originalThread)
    if (anchor.hasClass("post-reply-link") && number != null && localThread) {
        if (href == "#${number.value}") return PostLink.Reference(number)
    }
    val absolute = if (href.startsWith("//")) "https:$href" else href
    return RemoteUrl.parse(absolute)?.let(PostLink::External)
}
