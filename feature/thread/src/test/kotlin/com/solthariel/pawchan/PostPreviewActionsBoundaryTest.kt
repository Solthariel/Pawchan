package com.solthariel.pawchan

import androidx.compose.runtime.Composable
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.thread.PostMenuCommand
import com.solthariel.pawchan.thread.PostMenuTransfer
import com.solthariel.pawchan.thread.PostPreviewActions
import com.solthariel.pawchan.thread.executePreviewPostMenu
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PostPreviewActionsBoundaryTest {
    @Test
    fun replyIsPreparedBeforeClosingThePreviewAndNavigating() {
        val history = mutableListOf<String>()
        val actions = RecordingPreviewActions(history)
        val address = defaultThreadAddress(42)
        val post = toolsPost(7, "Текст")
        val content = toolsContent(listOf(post)).copy(address = address)
        val transfer = RecordingPreviewTransfer()
        executePreviewPostMenu(PostMenuCommand.REPLY, post, content, actions, transfer) { history += "close" }
        assertEquals(listOf("prepare", "close", "navigate"), history)
        assertEquals(address to ">>7\n", actions.reply)
        assertEquals(address, actions.destination)
        assertTrue(transfer.copied.isEmpty())
    }

    @Test
    fun quoteKeepsTheTextAndDoesNotSubmitAnything() {
        val actions = RecordingPreviewActions(mutableListOf())
        val address = defaultThreadAddress(42)
        val post = toolsPost(7, "Первая\nВторая")
        val content = toolsContent(listOf(post)).copy(address = address)
        executePreviewPostMenu(PostMenuCommand.QUOTE, post, content, actions, RecordingPreviewTransfer()) {}
        assertEquals(address to ">>7\n> Первая\n> Вторая\n", actions.reply)
    }

    @Test
    fun copyingHasNoNavigationOrPersistedSideEffects() {
        val history = mutableListOf<String>()
        val actions = RecordingPreviewActions(history)
        val address = defaultThreadAddress(42)
        val post = toolsPost(7, "Текст")
        val content = toolsContent(listOf(post)).copy(address = address)
        val transfer = RecordingPreviewTransfer()
        executePreviewPostMenu(PostMenuCommand.COPY_TEXT, post, content, actions, transfer) { history += "close" }
        executePreviewPostMenu(PostMenuCommand.COPY_LINK, post, content, actions, transfer) { history += "close" }
        assertEquals(listOf("Текст", publicThreadLink(address, 7)), transfer.copied)
        assertTrue(history.isEmpty())
    }
}

private class RecordingPreviewActions(
    private val history: MutableList<String>,
) : PostPreviewActions {
    var reply: Pair<ThreadAddress, String>? = null
    var destination: ThreadAddress? = null

    override fun prepareReply(
        address: ThreadAddress,
        text: String,
    ) {
        history += "prepare"
        reply = address to text
    }

    override fun navigate(address: ThreadAddress) {
        history += "navigate"
        destination = address
    }
}

private class RecordingPreviewTransfer : PostMenuTransfer {
    val copied = mutableListOf<String>()

    override fun postLink(
        address: ThreadAddress,
        postId: Long,
    ): String? = publicThreadLink(address, postId)

    override fun copy(text: String) {
        copied += text
    }
}
