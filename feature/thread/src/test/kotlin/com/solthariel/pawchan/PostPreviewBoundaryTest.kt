package com.solthariel.pawchan

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.thread.PostPreviewTarget
import com.solthariel.pawchan.thread.PostPreviewViewModel
import com.solthariel.pawchan.thread.ThreadUiState
import com.solthariel.pawchan.thread.toThreadUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PostPreviewBoundaryTest {
    @Test
    fun preparedRepositoryWorksWithoutWorkspaceControllerAndPreservesReadOnlyFlags() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val owner = ViewModelStore()
            try {
                val address = defaultThreadAddress(42)
                val repository = FakeThreadRepository(DomainOutcome.Success(snapshot(address, closed = true)))
                val factory = PostPreviewViewModel.factory(repository)
                val model = ViewModelProvider(owner, factory)[PostPreviewViewModel::class.java]
                model.load(PostPreviewTarget(address, 44), null)
                val result = model.state.first { it.content != null || it.failure != null }
                assertEquals(1, repository.requests)
                assertEquals(address, result.content?.address)
                assertEquals(listOf(44L), result.posts.map { it.id })
                assertTrue(requireNotNull(result.content).closed)
            } finally {
                owner.clear()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun matchingLocalPostDoesNotAskInjectedRepository() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val owner = ViewModelStore()
            try {
                val address = defaultThreadAddress(42)
                val repository = FakeThreadRepository()
                val factory = PostPreviewViewModel.factory(repository)
                val model = ViewModelProvider(owner, factory)[PostPreviewViewModel::class.java]
                val local = DomainOutcome.Success(snapshot(address)).toThreadUiState() as ThreadUiState.Content
                model.load(PostPreviewTarget(address, 44), local)
                val result = model.state.first { it.content != null || it.failure != null }
                assertEquals(0, repository.requests)
                assertEquals(listOf(44L), result.posts.map { it.id })
            } finally {
                owner.clear()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun absentRepositoryKeepsExplicitFailureInsteadOfInventingContent() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val owner = ViewModelStore()
            try {
                val factory = PostPreviewViewModel.factory(null)
                val model = ViewModelProvider(owner, factory)[PostPreviewViewModel::class.java]
                model.load(PostPreviewTarget(defaultThreadAddress(42), 44), null)
                val result = model.state.first { it.failure != null }
                assertTrue(result.posts.isEmpty())
                assertEquals(null, result.content)
                assertEquals("Не удалось загрузить пост", result.failure)
            } finally {
                owner.clear()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun returningToLoadedTargetDoesNotReloadOrClearItsState() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val owner = ViewModelStore()
            try {
                val address = defaultThreadAddress(42)
                val repository = FakeThreadRepository(DomainOutcome.Success(snapshot(address)))
                val factory = PostPreviewViewModel.factory(repository)
                val model = ViewModelProvider(owner, factory)[PostPreviewViewModel::class.java]
                val target = PostPreviewTarget(address, 44)
                model.load(target, null)
                val before = model.state.first { it.content != null }
                model.load(target, null)
                assertEquals(before, model.state.value)
                assertEquals(1, repository.requests)
            } finally {
                owner.clear()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun refreshingSameLocalTargetKeepsReadyContentUntilReplacementIsPrepared() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val owner = ViewModelStore()
            try {
                val address = defaultThreadAddress(42)
                val factory =
                    PostPreviewViewModel.factory(
                        null,
                        presentationDispatcher = StandardTestDispatcher(testScheduler),
                    )
                val model = ViewModelProvider(owner, factory)[PostPreviewViewModel::class.java]
                val local = DomainOutcome.Success(snapshot(address)).toThreadUiState() as ThreadUiState.Content
                val target = PostPreviewTarget(address, 44)
                model.load(target, local)
                val before = model.state.first { it.content != null }
                model.load(target, local.copy(subject = "updated"))
                assertEquals(before, model.state.value)
                val after = model.state.first { it.content?.subject == "updated" }
                assertEquals(before.posts, after.posts)
            } finally {
                owner.clear()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun localFactoryHasContentBeforeAnyCoroutineCanRun() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val owner = ViewModelStore()
            try {
                val address = defaultThreadAddress(42)
                val local = DomainOutcome.Success(snapshot(address)).toThreadUiState() as ThreadUiState.Content
                val target = PostPreviewTarget(address, 44)
                val factory = PostPreviewViewModel.factory(null, target = target, local = local)
                val model = ViewModelProvider(owner, factory)[PostPreviewViewModel::class.java]
                assertEquals(local, model.state.value.content)
                assertEquals(
                    listOf(44L),
                    model.state.value.posts
                        .map { it.id },
                )
                model.load(target, local)
                assertEquals(local, model.state.value.content)
            } finally {
                owner.clear()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun localEmptyRepliesDoNotShowAFalseLoadingWindow() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val owner = ViewModelStore()
            try {
                val address = defaultThreadAddress(42)
                val local = DomainOutcome.Success(snapshot(address)).toThreadUiState() as ThreadUiState.Content
                val target = PostPreviewTarget(address, 44, replies = true)
                val factory = PostPreviewViewModel.factory(null, target = target, local = local)
                val model = ViewModelProvider(owner, factory)[PostPreviewViewModel::class.java]
                assertEquals(local, model.state.value.content)
                assertTrue(
                    model.state.value.posts
                        .isEmpty(),
                )
                assertEquals(null, model.state.value.failure)
            } finally {
                owner.clear()
                Dispatchers.resetMain()
            }
        }

    private fun snapshot(
        address: ThreadAddress,
        closed: Boolean = false,
    ): ThreadSnapshot =
        ThreadSnapshot(
            address,
            "thread",
            ThreadFlags(pinned = false, closed = closed, archived = closed),
            listOf(
                BoardPost(
                    PostAddress(address, requireNotNull(PostId.parse(44))),
                    PostContent(PostDocument.plain("post"), PostMetadata(null, null, false)),
                    emptyList(),
                ),
            ),
        )
}
