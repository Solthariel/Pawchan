package com.solthariel.pawchan

import com.solthariel.pawchan.thread.ContentFilterSelection
import com.solthariel.pawchan.thread.PostContentAccess
import com.solthariel.pawchan.thread.PostMenuCommand
import com.solthariel.pawchan.thread.PostMenuEnvironment
import com.solthariel.pawchan.thread.PostMenuTransfer
import com.solthariel.pawchan.thread.ThreadActions
import com.solthariel.pawchan.thread.ThreadPostContext
import com.solthariel.pawchan.thread.ThreadPostPreferencesController
import com.solthariel.pawchan.thread.ThreadPresentation
import com.solthariel.pawchan.thread.ThreadToolsState
import com.solthariel.pawchan.thread.contentFilterSelection
import com.solthariel.pawchan.thread.executePostMenu
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadContentFilterActionTest {
    @Test
    fun opAndReplySelectionsUseTheBoardAndPlainTextNotAuthorOrThreadScope() {
        val address = defaultThreadAddress(42)
        assertFalse(toolsPost(42, "ОП").contentFilterSelection(address).comment)
        val reply = toolsPost(43, "голова дай денег").contentFilterSelection(address)
        assertTrue(reply.comment)
        assertEquals(address.board, reply.board)
        assertEquals("голова дай денег", reply.text)
    }

    @Test
    fun addingRuleOnlyOpensEditorAndDoesNotMutatePreferencesOrSendAReply() =
        runTest {
            val address = defaultThreadAddress(42)
            val post = toolsPost(43, "голова дай денег")
            val content = toolsContent(listOf(post))
            var selection: ContentFilterSelection? = null
            var replySent = false
            val preferences =
                ThreadPostPreferencesController(
                    address,
                    null,
                    PostContentAccess({ content }, {}),
                    StandardTestDispatcher(testScheduler),
                )
            val tools = ThreadToolsState(this)
            val environment =
                PostMenuEnvironment(
                    tools,
                    ThreadActions({ replySent = true }, {}, {}, {}),
                    ThreadPresentation(),
                    ThreadPostContext(address, preferences),
                    addFilter = { selection = it },
                )
            executePostMenu(PostMenuCommand.ADD_FILTER, post, environment, FilterEditorOnlyTransfer)
            assertEquals(post.contentFilterSelection(address), selection)
            assertFalse(replySent)
            assertNull(tools.notice)
            assertFalse(post.metadata.hidden)
        }
}

private object FilterEditorOnlyTransfer : PostMenuTransfer {
    override fun copy(text: String) {
        error("Adding a content rule must not copy text")
    }
}
