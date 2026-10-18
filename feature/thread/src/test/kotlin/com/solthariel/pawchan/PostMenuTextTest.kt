package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.model.PostTextStyle
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.thread.postQuote
import com.solthariel.pawchan.thread.toPostingMarkup
import org.junit.Assert.assertEquals
import org.junit.Test

class PostMenuTextTest {
    @Test
    fun quoteIncludesReferenceAndPrefixesEveryLine() {
        assertEquals(">>7\n> Первая\n> Вторая\n", postQuote(toolsPost(7, "Первая\nВторая")))
        val address = ThreadAddress(defaultBoardAddress(), requireNotNull(ThreadId.parse(42)))
        assertEquals("https://2ch.hk/${address.board.board.value}/res/42.html#7", publicThreadLink(address, 7))
    }

    @Test
    fun copyingMarkupRestoresSupportedNestedStyles() {
        val document =
            PostDocument(
                listOf(
                    PostTextRun("Секрет", linkedSetOf(PostTextStyle.BOLD, PostTextStyle.SPOILER)),
                    PostTextRun(" обычный"),
                ),
            )
        assertEquals("[b][spoiler]Секрет[/spoiler][/b] обычный", document.toPostingMarkup())
    }
}
