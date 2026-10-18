package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.thread.PostContentAccess
import com.solthariel.pawchan.thread.PostMenuCommand
import com.solthariel.pawchan.thread.PostMenuEnvironment
import com.solthariel.pawchan.thread.PostMenuTransfer
import com.solthariel.pawchan.thread.ThreadActions
import com.solthariel.pawchan.thread.ThreadPostContext
import com.solthariel.pawchan.thread.ThreadPostPreferencesController
import com.solthariel.pawchan.thread.ThreadPresentation
import com.solthariel.pawchan.thread.ThreadToolsState
import com.solthariel.pawchan.thread.executePostMenu
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PostMenuExecutorTest {
    @Test
    fun copyAndShareAreDifferentActionsAndReplyDoesNotSubmitAPost() =
        runTest {
            val address = ThreadAddress(defaultBoardAddress(), requireNotNull(ThreadId.parse(42)))
            val post = toolsPost(7, "Текст")
            val content = toolsContent(listOf(post))
            val tools = ThreadToolsState(this)
            val preferences =
                ThreadPostPreferencesController(
                    address,
                    null,
                    PostContentAccess({ content }, {}),
                    StandardTestDispatcher(testScheduler),
                )
            preferences.prepare(content)
            var inserted = ""
            val actions = ThreadActions({}, {}, {}, {}, onReplyTo = { inserted = it.orEmpty() })
            val environment =
                PostMenuEnvironment(
                    tools,
                    actions,
                    ThreadPresentation(),
                    ThreadPostContext(address, preferences),
                )
            val transfer = TestPostMenuTransfer()
            executePostMenu(PostMenuCommand.COPY_TEXT, post, environment, transfer)
            assertEquals(listOf("Текст"), transfer.copied)
            executePostMenu(PostMenuCommand.COPY_LINK, post, environment, transfer)
            assertEquals(listOf("Текст", publicThreadLink(address, 7)), transfer.copied)
            executePostMenu(PostMenuCommand.REPLY, post, environment, transfer)
            assertEquals(">>7\n", inserted)
        }

    @Test
    fun unpreparedPreferenceCommandsNeverFallThroughToCopyOrReply() =
        runTest {
            val post = toolsPost(7, "Текст")
            val content = toolsContent(listOf(post))
            val preferences =
                ThreadPostPreferencesController(
                    defaultThreadAddress(42),
                    null,
                    PostContentAccess({ content }, {}),
                    StandardTestDispatcher(testScheduler),
                )
            var inserted = ""
            val environment =
                PostMenuEnvironment(
                    ThreadToolsState(this),
                    ThreadActions({}, {}, {}, {}, onReplyTo = { inserted = it.orEmpty() }),
                    ThreadPresentation(),
                    ThreadPostContext(defaultThreadAddress(42), preferences),
                )
            val transfer = TestPostMenuTransfer()
            val commands =
                listOf(
                    PostMenuCommand.OWN,
                    PostMenuCommand.HIDE,
                    PostMenuCommand.HIDE_TREE,
                    PostMenuCommand.HIDE_NAME,
                    PostMenuCommand.HIDE_SIMILAR,
                    PostMenuCommand.SHOW,
                    PostMenuCommand.SHOW_ALL,
                )
            commands.forEach { command ->
                environment.tools.notice = null
                executePostMenu(command, post, environment, transfer)
                assertEquals("Не удалось сохранить отметку или правило скрытия", environment.tools.notice)
                assertEquals(emptyList<String>(), transfer.copied)
                assertEquals("", inserted)
                assertNull(preferences.currentRules.value)
            }
        }
}

private class TestPostMenuTransfer : PostMenuTransfer {
    val copied = mutableListOf<String>()

    override fun postLink(
        address: ThreadAddress,
        postId: Long,
    ): String? = publicThreadLink(address, postId)

    override fun copy(text: String) {
        copied.add(text)
    }
}
