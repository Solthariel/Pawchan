package com.solthariel.pawchan.infrastructure.thread.export

import com.solthariel.pawchan.domain.model.RemoteUrl
import com.solthariel.pawchan.thread.ThreadExportDocument

internal fun threadHtml(content: ThreadExportDocument): String =
    buildString {
        append("<!doctype html><html lang=\"ru\"><meta charset=\"utf-8\"><title>")
        append(escapeThreadHtml(content.subject ?: "Тред ${content.id}"))
        append("</title><style>")
        append("body{font-family:sans-serif;max-width:900px;margin:auto;padding:12px}")
        append("article{border-bottom:1px solid #aaa;padding:12px 0}")
        append(".head{font-size:12px;white-space:nowrap;overflow:auto}")
        append(".text{white-space:pre-wrap}img{max-width:72px;max-height:72px}</style><body>")
        content.posts.forEach { post ->
            append("<article><div class=\"head\">")
            append(post.index)
            append(' ')
            append(escapeThreadHtml(post.header.text))
            append("</div><div>")
            post.attachments.forEach { attachment ->
                if (RemoteUrl.parse(attachment.originalUrl) != null) {
                    append("<a href=\"")
                    append(escapeThreadHtml(attachment.originalUrl))
                    append("\">")
                    attachment.thumbnailUrl?.takeIf { RemoteUrl.parse(it) != null }?.let { thumbnail ->
                        append("<img alt=\"\" src=\"")
                        append(escapeThreadHtml(thumbnail))
                        append("\">")
                    }
                    append(escapeThreadHtml(attachment.fileName))
                    append("</a> ")
                }
            }
            append("</div>")
            post.subject
                ?.let { append("<h3>${escapeThreadHtml(it)}</h3>") }
            append("<div class=\"text\">${escapeThreadHtml(post.document.plainText)}</div></article>")
        }
        append("</body></html>")
    }

internal fun escapeThreadHtml(text: String): String =
    buildString {
        text.forEach { char ->
            append(
                when (char) {
                    '&' -> "&amp;"
                    '<' -> "&lt;"
                    '>' -> "&gt;"
                    '\"' -> "&quot;"
                    '\'' -> "&#39;"
                    else -> char.toString()
                },
            )
        }
    }
