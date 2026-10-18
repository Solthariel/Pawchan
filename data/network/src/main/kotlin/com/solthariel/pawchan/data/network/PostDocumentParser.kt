package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.model.PostTextStyle
import com.solthariel.pawchan.domain.model.RemoteUrl
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

internal object PostDocumentParser {
    fun parse(html: String): PostDocument {
        val runs = mutableListOf<PostTextRun>()
        Jsoup
            .parseBodyFragment(html)
            .body()
            .childNodes()
            .forEach { node -> appendNode(node, emptySet(), null, runs) }
        return PostDocument(runs.mergeAdjacent())
    }
}

private fun appendNode(
    node: Node,
    styles: Set<PostTextStyle>,
    link: PostLink?,
    runs: MutableList<PostTextRun>,
) {
    when (node) {
        is TextNode -> appendTextWithAutoLinks(node.text(), styles, link, runs)
        is Element -> appendElement(node, styles, link, runs)
    }
}

private fun appendTextWithAutoLinks(
    text: String,
    styles: Set<PostTextStyle>,
    parentLink: PostLink?,
    runs: MutableList<PostTextRun>,
) {
    if (parentLink != null) {
        runs += PostTextRun(text, styles, parentLink)
        return
    }
    var cursor = 0
    AUTO_LINK.findAll(text).forEach { match ->
        val start = match.range.first
        val urlText =
            match.value.trimEnd { character ->
                character in URL_TRAILING_PUNCTUATION
            }
        if (urlText.isEmpty()) return@forEach
        if (start > cursor) {
            runs += PostTextRun(text.substring(cursor, start), styles)
        }
        if (start > 0 && text[start - 1].isLetterOrDigit()) {
            runs += PostTextRun(" ", styles)
        }
        val normalizedUrl =
            when {
                urlText.startsWith("t.me/", ignoreCase = true) -> "https://$urlText"
                urlText.startsWith("www.", ignoreCase = true) -> "https://$urlText"
                else -> urlText
            }
        val autoLink = RemoteUrl.parse(normalizedUrl)?.let(PostLink::External)
        runs += PostTextRun(urlText, styles, autoLink)
        cursor = start + urlText.length
    }
    if (cursor < text.length) {
        runs += PostTextRun(text.substring(cursor), styles)
    }
}

private fun appendElement(
    element: Element,
    parentStyles: Set<PostTextStyle>,
    parentLink: PostLink?,
    runs: MutableList<PostTextRun>,
) {
    val tag = element.normalName()
    if (tag == "br") {
        runs += PostTextRun("\n", parentStyles, parentLink)
        return
    }
    val styles = parentStyles + tag.styles(element)
    val link = if (tag == "a") element.toPostLink() ?: parentLink else parentLink
    element.childNodes().forEach { child -> appendNode(child, styles, link, runs) }
    if (tag in BLOCK_TAGS && runs.lastOrNull()?.text?.endsWith('\n') == false) {
        runs += PostTextRun("\n", parentStyles)
    }
}

private fun String.styles(element: Element): Set<PostTextStyle> =
    buildSet {
        when (this@styles) {
            "b", "strong" -> add(PostTextStyle.BOLD)
            "i", "em" -> add(PostTextStyle.ITALIC)
            "u" -> add(PostTextStyle.UNDERLINE)
            "s", "strike", "del" -> add(PostTextStyle.STRIKETHROUGH)
        }
        val classes = element.classNames()
        if ("unkfunc" in classes) add(PostTextStyle.GREENTEXT)
        if (element.hasRedTextColor()) add(PostTextStyle.RED)
        if ("spoiler" in classes) add(PostTextStyle.SPOILER)
    }

private fun Element.hasRedTextColor(): Boolean {
    val normalized =
        attr("style")
            .lowercase()
            .filterNot(Char::isWhitespace)
    return RED_COLOR_DECLARATIONS.any(normalized::contains)
}

private fun Element.toPostLink(): PostLink? {
    val labelPostId =
        POST_REFERENCE
            .find(text())
            ?.groupValues
            ?.get(1)
            ?.toLongOrNull()
            ?.let(PostId::parse)
    if (labelPostId != null) {
        return PostLink.Reference(labelPostId, referenceThread(attr("href")))
    }
    val href = attr("href")
    val normalizedUrl = if (href.startsWith("//")) "https:$href" else href
    return RemoteUrl.parse(normalizedUrl)?.let(PostLink::External)
}

private fun List<PostTextRun>.mergeAdjacent(): List<PostTextRun> =
    fold(mutableListOf()) { merged, run ->
        val previous = merged.lastOrNull()
        if (previous != null && previous.styles == run.styles && previous.link == run.link) {
            merged[merged.lastIndex] = previous.copy(text = previous.text + run.text)
        } else {
            merged += run
        }
        merged
    }

private val POST_REFERENCE = Regex(""">>(\d+)""")
private val AUTO_LINK =
    Regex(
        pattern = """(?:https?://|www\.|t\.me/)[^\s<>"']+""",
        option = RegexOption.IGNORE_CASE,
    )
private val URL_TRAILING_PUNCTUATION = charArrayOf('.', ',', ';', ':', '!', '?', ')', ']', '}')
private val BLOCK_TAGS = setOf("p", "div", "blockquote", "pre")
private val RED_COLOR_DECLARATIONS =
    setOf(
        "color:red",
        "color:#f00",
        "color:#ff0000",
        "color:rgb(255,0,0)",
    )
