package com.solthariel.pawchan

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.ReplyComposerViewModel
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.navigation.defaultThreadAddress
import com.solthariel.pawchan.thread.ThreadPersistence
import com.solthariel.pawchan.thread.ThreadSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThreadComposerIntegrationTest {
    @Test
    fun submitsTextReplyAndRefreshesThread() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val owner = ViewModelStore()
            try {
                val address = defaultThreadAddress(42L)
                val result =
                    DomainOutcome.Success(
                        ThreadSnapshot(address, "Тред", ThreadFlags(false, false, false), emptyList()),
                    )
                val repository = IntegrationThreadRepository(result, result)
                val library = IntegrationThreadLibrary()
                val posting = FakePostingRepository()
                val viewModel =
                    ViewModelProvider(
                        owner,
                        ThreadSession.factory(
                            address,
                            repository,
                            ThreadPersistence(library, IntegrationWatchRepository()),
                        ),
                    )[ThreadSession::class.java]
                kotlinx.coroutines.withTimeout(5000L) {
                    while (library.visits < 1) kotlinx.coroutines.yield()
                }

                val factory = ReplyComposerViewModel.factory(address, posting, viewModel::onPostSubmitted)
                val reply = ViewModelProvider(owner, factory)[ReplyComposerViewModel::class.java].controller
                reply.open(threadClosed = false)
                advanceUntilIdle()
                reply.updateDraft(ComposerDraftState(comment = "Новый ответ"))
                reply.submit()
                advanceUntilIdle()
                kotlinx.coroutines.withTimeout(5000L) {
                    while (library.visits < 2) kotlinx.coroutines.yield()
                }

                assertEquals("Новый ответ", posting.lastDraft?.comment)
                assertEquals(false, reply.state.value.visible)
                assertEquals(2, repository.requests)
            } finally {
                owner.clear()
                Dispatchers.resetMain()
            }
        }
}
