package com.solthariel.pawchan

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.thread.ThreadRepository
import com.solthariel.pawchan.thread.PostPreviewTarget
import com.solthariel.pawchan.thread.PostPreviewViewModel
import com.solthariel.pawchan.thread.RichPostTextUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PostPreviewRefreshRaceTest {
    private val dispatcher = StandardTestDispatcher()
    private val owner = ViewModelStore()

    @Before fun setup() = Dispatchers.setMain(dispatcher)

    @After fun cleanup() {
        owner.clear()
        Dispatchers.resetMain()
    }

    @Test fun refreshWithoutTheSourcePostKeepsTheVisibleSnapshot() =
        runTest(dispatcher) {
            val model = model(FakeThreadRepository(DomainOutcome.Failure(DomainFailure.NotFound)))
            val target = PostPreviewTarget(null, 2)
            model.load(target, toolsContent(listOf(toolsPost(1), toolsPost(2, "keep me"))))
            runCurrent()
            val before = model.state.value
            assertEquals(listOf(2L), before.posts.map { it.id })
            model.load(target, toolsContent(listOf(toolsPost(1))))
            runCurrent()
            assertEquals(before, model.state.value)
        }

    @Test fun refreshThatTemporarilyHasNoRepliesDoesNotEmptyAnOpenRepliesWindow() =
        runTest(dispatcher) {
            val model = model(null)
            val target = PostPreviewTarget(null, 1, replies = true)
            val reference = PostLink.Reference(requireNotNull(PostId.parse(1)))
            val document = PostDocument(listOf(PostTextRun(">>1", link = reference)))
            val reply = toolsPost(2).copy(document = document, richText = RichPostTextUi.create(document))
            val local = toolsContent(listOf(toolsPost(1), reply))
            // Use the prepared reply index, exactly as the runtime does.
            model.load(target, local)
            runCurrent()
            val before = model.state.value
            assertEquals(listOf(2L), before.posts.map { it.id })
            model.load(target, toolsContent(listOf(toolsPost(1))))
            runCurrent()
            assertEquals(before, model.state.value)
        }

    @Test fun cancelledRemoteResponseCannotClearANewerLocalTarget() =
        runTest(dispatcher) {
            lateinit var pending: Continuation<DomainOutcome<ThreadSnapshot>>
            val repository =
                object : ThreadRepository {
                    override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> =
                        suspendCoroutine { pending = it }
                }
            val model = model(repository)
            model.load(PostPreviewTarget(defaultThreadAddress(42), 44), null)
            runCurrent()
            model.load(PostPreviewTarget(null, 2), toolsContent(listOf(toolsPost(2, "new target"))))
            runCurrent()
            pending.resume(DomainOutcome.Failure(DomainFailure.NetworkTimeout))
            runCurrent()
            assertEquals(
                listOf(2L),
                model.state.value.posts
                    .map { it.id },
            )
            assertNull(model.state.value.failure)
        }

    private fun model(repository: ThreadRepository?): PostPreviewViewModel =
        ViewModelProvider(
            owner,
            PostPreviewViewModel.factory(repository, presentationDispatcher = dispatcher),
        )[PostPreviewViewModel::class.java]
}
