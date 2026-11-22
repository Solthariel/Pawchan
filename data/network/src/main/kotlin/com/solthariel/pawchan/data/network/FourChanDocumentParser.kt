package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.model.PostTextStyle
import com.solthariel.pawchan.domain.model.RemoteUrl
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

internal fun parseFourChanDocument(html: String): PostDocument {
    val runs = mutableListOf<PostTextRun>()
    Jsoup.parseBodyFragment(html).body().childNodes().forEach { node ->
        appendFourChanNode(node, emptySet(), null, runs)
    }
    return PostDocument(runs)
}

private fun appendFourChanNode(
    node: Node,
    styles: Set<PostTextStyle>,
    link: PostLink?,
    out: MutableList<PostTextRun>,
) {
    when (node) {
        is TextNode -> {
            out += PostTextRun(node.wholeText, styles, link)
        }

        is Element -> {
            val tag = node.normalName()
            if (tag == "script" || tag == "style") return
            if (tag == "br") {
                out += PostTextRun("\n", styles, link)
                return
            }
            val nextStyles = styles + fourChanStyles(node)
            val nextLink = if (tag == "a") fourChanLink(node) ?: link else link
            node.childNodes().forEach { child -> appendFourChanNode(child, nextStyles, nextLink, out) }
            if (tag in setOf("p", "div", "blockquote") && out.lastOrNull()?.text?.endsWith('\n') == false) {
                out += PostTextRun("\n", styles)
            }
        }
    }
}

private fun fourChanStyles(element: Element): Set<PostTextStyle> =
    buildSet {
        when (element.normalName()) {
            "b", "strong" -> add(PostTextStyle.BOLD)
            "i", "em" -> add(PostTextStyle.ITALIC)
            "u" -> add(PostTextStyle.UNDERLINE)
            "s", "strike", "del" -> add(PostTextStyle.SPOILER)
        }
        if (element.hasClass("quote")) add(PostTextStyle.GREENTEXT)
        if (element.hasClass("spoiler")) add(PostTextStyle.SPOILER)
    }

private fun fourChanLink(element: Element): PostLink? {
    val href = element.attr("href")
    val local =
        FOUR_CHAN_LOCAL
            .matchEntire(href)
            ?.groups
            ?.get("post")
            ?.value
            ?.toLongOrNull()
            ?.let(PostId::parse)
    return if (local != null) {
        PostLink.Reference(local)
    } else {
        val normalized = if (href.startsWith("//")) "https:$href" else href
        crossThreadLink(href) ?: RemoteUrl.parse(normalized)?.let(PostLink::External)
    }
}

private fun crossThreadLink(href: String): PostLink.Reference? {
    val match = FOUR_CHAN_THREAD.matchEntire(href)
    val board =
        match
            ?.groups
            ?.get("board")
            ?.value
            ?.let(BoardId::parse)
    val thread =
        match
            ?.groups
            ?.get("thread")
            ?.value
            ?.toLongOrNull()
            ?.let(ThreadId::parse)
    val post =
        match
            ?.groups
            ?.get("post")
            ?.value
            ?.toLongOrNull()
            ?.let(PostId::parse)
    return if (board != null && thread != null && post != null) {
        PostLink.Reference(post, ThreadAddress(BoardAddress(FOUR_CHAN_SITE, board), thread))
    } else {
        null
    }
}

private val FOUR_CHAN_LOCAL = Regex("#p(?<post>[0-9]+)")
private val FOUR_CHAN_THREAD =
    Regex("(?:https://boards\\.4chan(?:nel)?\\.org)?/(?<board>[a-z0-9]+)/thread/(?<thread>[0-9]+)#p(?<post>[0-9]+)")
