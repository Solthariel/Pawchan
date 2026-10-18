package com.solthariel.pawchan.composer

internal enum class CommentMarkup(
    val label: String,
    val tag: String?,
) {
    BOLD("B", "b"),
    ITALIC("I", "i"),
    UNDERLINE("U", "u"),
    OVERLINE("O̅", "o"),
    STRIKE("S", "s"),
    SUBSCRIPT("SUB", "sub"),
    SUPERSCRIPT("SUP", "sup"),
    SPOILER("SP", "spoiler"),
    QUOTE(">", null),
}

internal data class CommentEdit(
    val text: String,
    val selectionStart: Int,
    val selectionEnd: Int,
)

internal fun applyCommentMarkup(
    edit: CommentEdit,
    markup: CommentMarkup,
): CommentEdit {
    val start = minOf(edit.selectionStart, edit.selectionEnd).coerceIn(0, edit.text.length)
    val end = maxOf(edit.selectionStart, edit.selectionEnd).coerceIn(start, edit.text.length)
    val tag = markup.tag
    return if (tag == null) {
        quoteComment(edit.text, start, end)
    } else {
        val opening = "[$tag]"
        val closing = "[/$tag]"
        toggleCommentTags(edit.text, start, end, opening, closing)
    }
}

private fun quoteComment(
    text: String,
    start: Int,
    end: Int,
): CommentEdit {
    val selected = text.substring(start, end)
    val unquote = selected.startsWith('>')
    val newLine = !unquote && start > 0 && text[start - 1] != '\n'
    val prefix = if (newLine) "\n> " else "> "
    val replacement =
        if (unquote) {
            selected.split('\n').joinToString("\n", transform = ::removeQuotePrefix)
        } else {
            prefix + quoteLineBreaks(selected)
        }
    val caretEnd = start + replacement.length
    val selectionStart = if (unquote) start else start + if (newLine) 1 else 0
    val caretStart = if (!unquote && caretEnd - selectionStart <= 2) caretEnd else selectionStart
    return CommentEdit(text.replaceRange(start, end, replacement), caretStart, caretEnd)
}

private fun removeQuotePrefix(line: String): String =
    when {
        line.startsWith("> ") -> line.drop(2)
        line.startsWith('>') -> line.drop(1)
        else -> line
    }

private fun toggleCommentTags(
    text: String,
    start: Int,
    end: Int,
    opening: String,
    closing: String,
): CommentEdit {
    val selected = text.substring(start, end)
    val before = (start - opening.length).coerceAtLeast(0)
    val after = (end + closing.length).coerceAtMost(text.length)
    val wrapped =
        text.substring(before, start).equals(opening, ignoreCase = true) &&
            text.substring(end, after).equals(closing, ignoreCase = true)
    return if (wrapped) {
        CommentEdit(text.replaceRange(before, after, selected), before, end - opening.length)
    } else {
        val replacement = text.replaceRange(start, end, "$opening$selected$closing")
        CommentEdit(replacement, start + opening.length, end + opening.length)
    }
}

private fun quoteLineBreaks(text: String): String =
    buildString {
        var afterBreak = false
        for (char in text) {
            if (afterBreak && char != '\n') append("> ")
            append(char)
            afterBreak = char == '\n'
        }
        if (afterBreak) append("> ")
    }
