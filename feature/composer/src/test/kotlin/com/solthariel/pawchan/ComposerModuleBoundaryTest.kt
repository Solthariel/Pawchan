package com.solthariel.pawchan

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.solthariel.pawchan.composer.ComposerDraftCoordinator
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.NewThreadViewModel
import com.solthariel.pawchan.composer.ReplyComposerViewModel
import com.solthariel.pawchan.domain.drafts.ComposerDraftKey
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ComposerModuleBoundaryTest {
    @Test
    fun publicNewThreadFactoryPreservesDeferredOpen() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val owner = ViewModelStore()
            try {
                val posting = FakePostingRepository()
                val factory = NewThreadViewModel.factory(defaultBoardAddress(), posting, autoOpen = false)
                val model = ViewModelProvider(owner, factory)[NewThreadViewModel::class.java]
                advanceUntilIdle()
                assertEquals(0, posting.captchaLoads)
                assertEquals("", model.state.value.comment)
                assertFalse(model.state.value.progress.submitting)
            } finally {
                owner.clear()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun appOwnedDraftWriteSurvivesRouteOwnerClearAndRestoresInNewOwner() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val first = ViewModelStore()
            val next = ViewModelStore()
            val routeJobs = mutableListOf<Job>()
            try {
                val address = defaultThreadAddress(42)
                val disk = TestComposerDraftRepository()
                val storage = ComposerDraftCoordinator.create(this, disk)
                val factory = ReplyComposerViewModel.factory(address, FakePostingRepository(), {}, storage)
                val firstModel = ViewModelProvider(first, factory)[ReplyComposerViewModel::class.java]
                firstModel.viewModelScope.coroutineContext[Job]?.let(routeJobs::add)
                val reply = firstModel.controller
                reply.open(false)
                advanceUntilIdle()
                reply.updateDraft(ComposerDraftState(comment = "route draft"))
                reply.saveDraftNow()
                first.clear()
                routeJobs.forEach { it.join() }
                advanceUntilIdle()
                assertEquals("route draft", disk.saved[ComposerDraftKey(address.board, address.thread)]?.text?.comment)
                val nextModel = ViewModelProvider(next, factory)[ReplyComposerViewModel::class.java]
                nextModel.viewModelScope.coroutineContext[Job]?.let(routeJobs::add)
                val restored = nextModel.controller
                restored.open(false)
                val result = restored.state.first { it.comment == "route draft" }
                advanceUntilIdle()
                assertEquals("route draft", result.comment)
            } finally {
                first.clear()
                next.clear()
                routeJobs.forEach { it.join() }
                Dispatchers.resetMain()
            }
        }

    @Test
    fun equalBoardAndThreadNumbersFromDifferentSitesKeepSeparateDraftKeys() =
        runTest {
            val disk = TestComposerDraftRepository()
            val storage = ComposerDraftCoordinator.create(this, disk)
            for (site in listOf("site-a", "site-b")) {
                val board = BoardAddress(requireNotNull(SiteId.parse(site)), requireNotNull(BoardId.parse("b")))
                val address = ThreadAddress(board, requireNotNull(ThreadId.parse(42)))
                storage.schedule(
                    ComposerDraftKey(address.board, address.thread),
                    ComposerDraftState(comment = site),
                    true,
                )
            }
            advanceUntilIdle()
            assertEquals(2, disk.saved.size)
            assertEquals(
                setOf("site-a", "site-b"),
                disk.saved.values
                    .map { it.text.comment }
                    .toSet(),
            )
        }
}
