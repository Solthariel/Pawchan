package com.solthariel.pawchan.thread

internal enum class ThreadSearchFlag(
    val token: String,
    val title: String,
) {
    MY("m", "Мои посты"),
    REPLIES("r", "Ответы мне"),
    ATTACHMENTS("a", "С вложениями"),
    DELETED("d", "Удалённые"),
    EDITED("e", "Изменённые"),
    NEW("n", "Новые"),
    OP("op", "Посты автора треда"),
}

internal data class ThreadSearchTerm(
    val text: String,
    val excluded: Boolean,
    val flag: ThreadSearchFlag?,
)

internal fun parseThreadSearch(query: String): List<ThreadSearchTerm> =
    SEARCH_PART
        .findAll(query)
        .map { match ->
            val raw = match.value
            val excluded = raw.startsWith("-") || raw.startsWith(":-")
            val text =
                raw
                    .removePrefix(":")
                    .removePrefix("-")
                    .removeSurrounding("\"")
                    .lowercase()
            ThreadSearchTerm(
                text,
                excluded,
                if (raw.startsWith(":")) ThreadSearchFlag.entries.find { it.token == text } else null,
            )
        }.toList()

internal fun matchesThreadSearch(
    post: ThreadPostUi,
    terms: List<ThreadSearchTerm>,
    flag: ThreadSearchFlag?,
): Boolean {
    if (post.metadata.hidden || (flag != null && !post.matchesFlag(flag))) return false
    val text =
        listOf(
            post.id.toString(),
            post.author.orEmpty(),
            post.metadata.display.subject
                .orEmpty(),
            post.document.plainText,
        ).joinToString(" ")
            .lowercase()
    return terms.all { term ->
        val found = term.flag?.let(post::matchesFlag) ?: text.contains(term.text)
        if (term.excluded) !found else found
    }
}

private fun ThreadPostUi.matchesFlag(flag: ThreadSearchFlag): Boolean =
    when (flag) {
        ThreadSearchFlag.MY -> metadata.changes.own
        ThreadSearchFlag.REPLIES -> metadata.changes.replyToMe
        ThreadSearchFlag.ATTACHMENTS -> attachments.isNotEmpty()
        ThreadSearchFlag.DELETED -> metadata.changes.deleted
        ThreadSearchFlag.EDITED -> metadata.changes.edited
        ThreadSearchFlag.NEW -> metadata.changes.new
        ThreadSearchFlag.OP -> metadata.changes.originalPoster
    }

private val SEARCH_PART = Regex("(?::?-)?\"[^\"]*\"|[^\\s]+")
