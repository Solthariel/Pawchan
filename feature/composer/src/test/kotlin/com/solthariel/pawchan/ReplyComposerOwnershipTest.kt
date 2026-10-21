package com.solthariel.pawchan

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.ReplyComposerViewModel
import com.solthariel.pawchan.composer.ReplyError
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.posting.PostingFailure
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.posting.PostingProgressObserver
import com.solthariel.pawchan.domain.posting.PostingRepository
import com.solthariel.pawchan.domain.posting.ReplyDraft
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReplyComposerOwnershipTest {
    @Test
    fun ownerReuseKeepsTheSameComposerAndItsDraft() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val store = ViewModelStore()
            try {
                val factory = ReplyComposerViewModel.factory(defaultThreadAddress(42), FakePostingRepository(), {})
                val first = ViewModelProvider(store, factory)["reply", ReplyComposerViewModel::class.java]
                first.controller.open(false)
                advanceUntilIdle()
                first.controller.updateDraft(ComposerDraftState(comment = "Сохранить сессию"))
                val retained = ViewModelProvider(store, factory)["reply", ReplyComposerViewModel::class.java]
                assertSame(first, retained)
                assertEquals("Сохранить сессию", retained.controller.state.value.comment)
            } finally {
                store.clear()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun clearingTheOwnerCancelsPostingWithoutDeliveringSuccess() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val store = ViewModelStore()
            try {
                val repository = GatedReplyRepository()
                var deliveries = 0
                val factory = ReplyComposerViewModel.factory(defaultThreadAddress(42), repository, { deliveries++ })
                val model = ViewModelProvider(store, factory)["reply", ReplyComposerViewModel::class.java]
                model.controller.open(false)
                advanceUntilIdle()
                model.controller.updateDraft(ComposerDraftState(comment = "Ответ"))
                model.controller.submit()
                runCurrent()
                assertTrue(repository.started)
                store.clear()
                advanceUntilIdle()
                assertTrue(repository.cancelled)
                assertEquals(0, deliveries)
            } finally {
                store.clear()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun postingDeliversAnAddressedPreviewOnceInsteadOfMutableLastResultFields() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val store = ViewModelStore()
            try {
                val address = defaultThreadAddress(42)
                val deliveries = mutableListOf<BoardPost>()
                val factory = ReplyComposerViewModel.factory(address, FakePostingRepository(), { deliveries.add(it) })
                val model = ViewModelProvider(store, factory)["reply", ReplyComposerViewModel::class.java]
                model.controller.open(false)
                advanceUntilIdle()
                model.controller.updateDraft(ComposerDraftState(comment = "Ответ", email = "sage"))
                model.controller.submit()
                runCurrent()
                model.controller.submit()
                assertEquals(1, deliveries.size)
                val posted = deliveries.single()
                assertEquals(address, posted.address.thread)
                assertEquals(44L, posted.address.post.value)
                assertEquals("Ответ", posted.content.document.plainText)
                assertTrue(posted.content.metadata.sage)
                advanceUntilIdle()
            } finally {
                store.clear()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun postingFailureNeverDeliversASuccessResult() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val store = ViewModelStore()
            try {
                val repository = GatedReplyRepository()
                repository.result.complete(PostingOutcome.Failure(PostingFailure.NetworkUnavailable))
                var deliveries = 0
                val factory = ReplyComposerViewModel.factory(defaultThreadAddress(42), repository, { deliveries++ })
                val model = ViewModelProvider(store, factory)["reply", ReplyComposerViewModel::class.java]
                model.controller.open(false)
                advanceUntilIdle()
                model.controller.updateDraft(ComposerDraftState(comment = "Ответ"))
                model.controller.submit()
                advanceUntilIdle()
                assertEquals(0, deliveries)
                assertEquals(ReplyError.NETWORK, model.controller.state.value.error)
                assertTrue(model.controller.state.value.visible)
                assertEquals("Ответ", model.controller.state.value.comment)
            } finally {
                store.clear()
                Dispatchers.resetMain()
            }
        }
}

private class GatedReplyRepository : PostingRepository by FakePostingRepository() {
    val result = CompletableDeferred<PostingOutcome<PostId>>()
    var started = false
    var cancelled = false

    override suspend fun submitReply(
        address: ThreadAddress,
        draft: ReplyDraft,
        observer: PostingProgressObserver,
    ): PostingOutcome<PostId> {
        started = true
        return try {
            result.await()
        } finally {
            cancelled = !result.isCompleted
        }
    }
}
