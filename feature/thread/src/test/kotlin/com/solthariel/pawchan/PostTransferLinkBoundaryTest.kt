package com.solthariel.pawchan

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
import org.junit.Assert.assertTrue
import org.junit.Test

class PostTransferLinkBoundaryTest {
    @Test fun absentLinkCapabilityDoesNotCopyOrShareAFakeUrlOrClaimSuccess() =
        runTest {
            val post = toolsPost(7, "Текст")
            val content = toolsContent(listOf(post))
            val address = defaultThreadAddress(42)
            val preferences =
                ThreadPostPreferencesController(
                    address,
                    null,
                    PostContentAccess({ content }, {}),
                    StandardTestDispatcher(testScheduler),
                )
            val environment =
                PostMenuEnvironment(
                    ThreadToolsState(this),
                    ThreadActions({}, {}, {}, {}),
                    ThreadPresentation(),
                    ThreadPostContext(address, preferences),
                )
            val transfer = NoLinkTransfer()
            executePostMenu(PostMenuCommand.COPY_LINK, post, environment, transfer)
            assertTrue(transfer.copied.isEmpty())
            assertNull(environment.tools.notice)
        }

    @Test fun plainTextCopyRemainsAvailableWithoutASiteLinkCapability() =
        runTest {
            val post = toolsPost(7, "Текст")
            val content = toolsContent(listOf(post))
            val address = defaultThreadAddress(42)
            val preferences =
                ThreadPostPreferencesController(
                    address,
                    null,
                    PostContentAccess({ content }, {}),
                    StandardTestDispatcher(testScheduler),
                )
            val environment =
                PostMenuEnvironment(
                    ThreadToolsState(this),
                    ThreadActions({}, {}, {}, {}),
                    ThreadPresentation(),
                    ThreadPostContext(address, preferences),
                )
            val transfer = NoLinkTransfer()
            executePostMenu(PostMenuCommand.COPY_TEXT, post, environment, transfer)
            assertEquals(listOf("Текст"), transfer.copied)
            assertEquals("Скопировано", environment.tools.notice)
        }
}

private class NoLinkTransfer : PostMenuTransfer {
    val copied = mutableListOf<String>()

    override fun copy(text: String) {
        copied += text
    }
}
