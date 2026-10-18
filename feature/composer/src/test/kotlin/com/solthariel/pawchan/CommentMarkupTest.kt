package com.solthariel.pawchan

import com.solthariel.pawchan.composer.CommentEdit
import com.solthariel.pawchan.composer.CommentMarkup
import com.solthariel.pawchan.composer.applyCommentMarkup
import org.junit.Assert.assertEquals
import org.junit.Test

class CommentMarkupTest {
    @Test
    fun wrapsSelectedTextWithEverySupportedTag() {
        CommentMarkup.entries.filter { it.tag != null }.forEach { markup ->
            val result = applyCommentMarkup(CommentEdit("abc", 0, 3), markup)
            assertEquals("[${markup.tag}]abc[/${markup.tag}]", result.text)
            assertEquals(markup.tag!!.length + 2, result.selectionStart)
            assertEquals(3, result.selectionEnd - result.selectionStart)
        }
    }

    @Test
    fun noSelectionPlacesCaretBetweenTagsAndPreservesOtherText() {
        val result = applyCommentMarkup(CommentEdit("ab", 1, 1), CommentMarkup.BOLD)
        assertEquals("a[b][/b]b", result.text)
        assertEquals(4, result.selectionStart)
        assertEquals(4, result.selectionEnd)
    }

    @Test
    fun quotesEachSelectedLineAndNormalizesReversedSelection() {
        val result = applyCommentMarkup(CommentEdit("one\ntwo\nthree", 7, 0), CommentMarkup.QUOTE)
        assertEquals("> one\n> two\nthree", result.text)
        val reverse = applyCommentMarkup(CommentEdit("abc", 3, 0), CommentMarkup.SPOILER)
        assertEquals("[spoiler]abc[/spoiler]", reverse.text)
    }

    @Test
    fun emptyQuoteStartsANewQuoteWithoutLosingCaret() {
        assertEquals(CommentEdit("> ", 2, 2), applyCommentMarkup(CommentEdit("", 0, 0), CommentMarkup.QUOTE))
    }

    @Test fun emptyTagPairsToggleOffAndOnForEverySupportedStyle() {
        CommentMarkup.entries.filter { it.tag != null }.forEach { markup ->
            val original = CommentEdit("ab", 1, 1)
            val applied = applyCommentMarkup(original, markup)
            val removed = applyCommentMarkup(applied, markup)
            assertEquals(original, removed)
            assertEquals(applied, applyCommentMarkup(removed, markup))
        }
    }

    @Test fun secondPressUnwrapsSelectedTextWithoutDeletingItOrItsNeighbors() {
        CommentMarkup.entries.filter { it.tag != null }.forEach { markup ->
            val original = CommentEdit("before text after", 7, 11)
            assertEquals(original, applyCommentMarkup(applyCommentMarkup(original, markup), markup))
        }
    }

    @Test fun nestedStylesRemoveOnlyTheRequestedPairAndKeepTheCaret() {
        val bold = applyCommentMarkup(CommentEdit("", 0, 0), CommentMarkup.BOLD)
        val italic = applyCommentMarkup(bold, CommentMarkup.ITALIC)
        assertEquals("[b][i][/i][/b]", italic.text)
        assertEquals(bold, applyCommentMarkup(italic, CommentMarkup.ITALIC))
        assertEquals(CommentEdit("", 0, 0), applyCommentMarkup(bold, CommentMarkup.BOLD))
    }

    @Test fun matchingTagsAreCaseInsensitiveButMismatchedTagsAreNotRemoved() {
        assertEquals(
            CommentEdit("text", 0, 4),
            applyCommentMarkup(CommentEdit("[B]text[/B]", 3, 7), CommentMarkup.BOLD),
        )
        assertEquals("[b][b][/b][/i]", applyCommentMarkup(CommentEdit("[b][/i]", 3, 3), CommentMarkup.BOLD).text)
    }

    @Test fun repeatingQuoteUnquotesTheSelectedBlockWithoutChangingOtherLines() {
        val original = CommentEdit("one\ntwo\nlast", 0, 7)
        val quoted = applyCommentMarkup(original, CommentMarkup.QUOTE)
        assertEquals(original, applyCommentMarkup(quoted, CommentMarkup.QUOTE))
    }

    @Test fun quotingInTheMiddleOfALineAddsANewLineAndPreservesUnselectedText() {
        val quoted = applyCommentMarkup(CommentEdit("before after", 7, 7), CommentMarkup.QUOTE)
        assertEquals(CommentEdit("before \n> after", 10, 10), quoted)
    }

    @Test fun quotationPreservesRunsOfBlankLinesWithoutExtraPrefixes() {
        val original = CommentEdit("one\n\ntwo", 0, 8)
        val quoted = applyCommentMarkup(original, CommentMarkup.QUOTE)
        assertEquals("> one\n\n> two", quoted.text)
        assertEquals(original, applyCommentMarkup(quoted, CommentMarkup.QUOTE))
    }
}
