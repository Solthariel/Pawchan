package com.solthariel.pawchan

import com.solthariel.pawchan.composer.ComposerDraftCoordinator
import com.solthariel.pawchan.composer.ComposerDraftSession
import com.solthariel.pawchan.composer.ComposerDraftSessionActions
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.domain.drafts.ComposerDraftKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ComposerClosingStateTest {
    @Test fun repeatedBackCoalescesAndKeepsContentWithoutAnAttachmentSpinner() =
        runTest {
            val fixture = ClosingFixture(this, StandardTestDispatcher(testScheduler))
            fixture.close()
            fixture.close()
            assertTrue(fixture.draft.closing)
            assertTrue(fixture.draft.inputBlocked)
            assertFalse(fixture.draft.filesLoading)
            assertEquals("keep", fixture.draft.comment)
            runCurrent()
            assertEquals(1, fixture.closed)
            assertEquals(1, fixture.repository.writes)
            assertFalse(fixture.draft.closing)
            assertNull(fixture.error)
        }

    @Test fun failedSaveUnblocksTheSameDraftAndRetryCanCloseIt() =
        runTest {
            val fixture = ClosingFixture(this, StandardTestDispatcher(testScheduler))
            fixture.repository.failWrites = true
            fixture.close()
            runCurrent()
            assertEquals(0, fixture.closed)
            assertEquals("keep", fixture.draft.comment)
            assertFalse(fixture.draft.inputBlocked)
            assertTrue(fixture.error.orEmpty().contains("сохранить"))
            fixture.repository.failWrites = false
            fixture.close()
            runCurrent()
            assertEquals(1, fixture.closed)
        }
}

private class ClosingFixture(
    scope: CoroutineScope,
    dispatcher: TestDispatcher,
) {
    val repository = TestComposerDraftRepository()
    var draft = ComposerDraftState(comment = "keep")
    var error: String? = null
    var closed = 0
    private val session =
        ComposerDraftSession(
            scope,
            ComposerDraftCoordinator(scope, repository, restoreDispatcher = dispatcher),
            ComposerDraftKey(defaultBoardAddress()),
            ComposerDraftSessionActions({ draft }, { draft = it }, { error = it }),
        )

    fun close() = session.close(draft) { closed++ }
}
