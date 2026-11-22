package com.solthariel.pawchan

import com.solthariel.pawchan.composer.ComposerDraftCoordinator
import com.solthariel.pawchan.composer.ComposerDraftSession
import com.solthariel.pawchan.composer.ComposerDraftSessionActions
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.domain.drafts.ComposerDraftKey
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ComposerClosePresentationTest {
    @Test fun openingADraftDoesNotPretendToImportAttachments() =
        runTest {
            val coordinator =
                ComposerDraftCoordinator(
                    this,
                    TestComposerDraftRepository(),
                    { testScheduler.currentTime },
                    StandardTestDispatcher(testScheduler),
                )
            var draft = ComposerDraftState()
            val session =
                ComposerDraftSession(
                    this,
                    coordinator,
                    ComposerDraftKey(defaultBoardAddress()),
                    ComposerDraftSessionActions({ draft }, { draft = it }, {}),
                )
            session.restore()
            assertFalse("Restoring a draft is not attachment import", draft.filesLoading)
            org.junit.Assert.assertTrue(draft.inputBlocked)
            session.awaitRestore()
            assertFalse(draft.inputBlocked)
        }

    @Test fun savingOnCloseDoesNotPretendToImportAttachments() =
        runTest {
            val repository = TestComposerDraftRepository()
            val coordinator =
                ComposerDraftCoordinator(
                    this,
                    repository,
                    { testScheduler.currentTime },
                    StandardTestDispatcher(testScheduler),
                )
            var draft = ComposerDraftState(comment = "keep text while closing")
            val session =
                ComposerDraftSession(
                    this,
                    coordinator,
                    ComposerDraftKey(defaultBoardAddress()),
                    ComposerDraftSessionActions({ draft }, { draft = it }, {}),
                )
            session.close(draft) {}
            assertFalse("Closing is not attachment loading", draft.filesLoading)
            assertEquals("keep text while closing", draft.comment)
        }
}
