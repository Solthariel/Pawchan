package com.solthariel.pawchan

import com.solthariel.pawchan.core.ui.RefreshFailure
import com.solthariel.pawchan.domain.model.AttachmentId
import com.solthariel.pawchan.domain.model.AttachmentSource
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.ByteSize
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.MediaAttachment
import com.solthariel.pawchan.domain.model.MediaDimensions
import com.solthariel.pawchan.domain.model.MediaKind
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.RemoteUrl
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.thread.ThreadError
import com.solthariel.pawchan.thread.ThreadPersistence
import com.solthariel.pawchan.thread.ThreadUiState
import com.solthariel.pawchan.thread.ThreadViewModel
import com.solthariel.pawchan.thread.ThreadViewModelEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThreadViewModelTest {
    @Test
    fun loadsThreadIntoContentState() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val address = defaultThreadAddress(42L)
                val repository = FakeThreadRepository(success(address))
                val library = FakeThreadLibrary()
                val viewModel =
                    ThreadViewModel(
                        address,
                        repository,
                        ThreadPersistence(library, FakeWatchRepository()),
                    )

                advanceUntilIdle()

                val content = viewModel.state.value as ThreadUiState.Content
                assertEquals("Тестовый тред", content.subject)
                val post = content.posts.single()
                val image = post.attachments.first()
                val video = post.attachments.last()
                assertEquals(43L, post.id)
                assertEquals("Ответ", post.document.plainText)
                assertEquals("Аноним", post.author)
                assertEquals("cat.jpg", image.fileName)
                assertEquals("https://2ch.su/src/cat.jpg", image.originalUrl)
                assertEquals("https://2ch.su/thumb/cat.jpg", image.thumbnailUrl)
                assertEquals(12L * 1024L, image.byteSize)
                assertEquals(AttachmentViewer.IMAGE, image.viewer)
                assertEquals(1024, image.dimensions?.width)
                assertEquals(768, image.dimensions?.height)
                assertEquals(AttachmentViewer.VIDEO, video.viewer)
                assertEquals(false, content.isFavorite)
                assertEquals(1, repository.requests)
                assertEquals(1, library.visits)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun refreshKeepsContentAndCancelsPreviousRequest() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val address = defaultThreadAddress(42L)
                val repository = RefreshThreadRepository(success(address))
                val viewModel =
                    ThreadViewModel(
                        address,
                        repository,
                        ThreadPersistence(FakeThreadLibrary(), FakeWatchRepository()),
                    )
                advanceUntilIdle()

                viewModel.refresh()
                runCurrent()
                assertEquals(true, (viewModel.state.value as ThreadUiState.Content).isRefreshing)

                viewModel.refresh()
                advanceUntilIdle()

                assertEquals(false, (viewModel.state.value as ThreadUiState.Content).isRefreshing)
                assertEquals(1, repository.cancellations)
                assertEquals(3, repository.requests)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun togglesFavoriteAndPersistsIt() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val address = defaultThreadAddress(42L)
                val library = FakeThreadLibrary()
                val viewModel =
                    ThreadViewModel(
                        address,
                        FakeThreadRepository(success(address)),
                        ThreadPersistence(library, FakeWatchRepository()),
                    )
                advanceUntilIdle()

                viewModel.setFavorite(true)
                advanceUntilIdle()

                assertEquals(true, (viewModel.state.value as ThreadUiState.Content).isFavorite)
                assertEquals(true, library.favorite)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun failedRefreshKeepsReadableThread() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val address = defaultThreadAddress(42L)
                val repository =
                    FakeThreadRepository(
                        success(address),
                        DomainOutcome.Failure(DomainFailure.NetworkTimeout),
                    )
                val viewModel =
                    ThreadViewModel(
                        address,
                        repository,
                        ThreadPersistence(FakeThreadLibrary(), FakeWatchRepository()),
                    )
                advanceUntilIdle()
                val old = viewModel.state.value as ThreadUiState.Content
                viewModel.refresh()
                advanceUntilIdle()
                val content = viewModel.state.value as ThreadUiState.Content
                assertEquals(old.posts, content.posts)
                assertEquals(false, content.isRefreshing)
                assertEquals(RefreshFailure(), content.presentation.refreshFailure)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun retryReplacesErrorWithFreshContent() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val address = defaultThreadAddress(42L)
                val repository =
                    FakeThreadRepository(
                        DomainOutcome.Failure(DomainFailure.NetworkUnavailable),
                        success(address),
                    )
                val viewModel =
                    ThreadViewModel(
                        address,
                        repository,
                        ThreadPersistence(FakeThreadLibrary(), FakeWatchRepository()),
                    )
                advanceUntilIdle()
                assertEquals(ThreadError.NETWORK, (viewModel.state.value as ThreadUiState.Error).reason)

                viewModel.refresh()
                advanceUntilIdle()

                assertEquals("Тестовый тред", (viewModel.state.value as ThreadUiState.Content).subject)
                assertEquals(2, repository.requests)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun watcherAddsNewPostsWithoutReplacingContentWithSpinner() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val address = defaultThreadAddress(42L)
                val repository =
                    FakeThreadRepository(
                        success(address),
                        success(address, listOf(43L, 44L)),
                    )
                val viewModel =
                    ThreadViewModel(
                        address = address,
                        repository = repository,
                        persistence = ThreadPersistence(FakeThreadLibrary(), FakeWatchRepository()),
                        environment = ThreadViewModelEnvironment(watchIntervalMillis = 1_000L),
                    )
                advanceUntilIdle()

                viewModel.toggleWatcher()
                runCurrent()
                viewModel.watcher.setActive(true)
                advanceTimeBy(1_000L)
                runCurrent()

                assertEquals(2, (viewModel.state.value as ThreadUiState.Content).posts.size)
                assertEquals(1, viewModel.watcher.state.value.newPosts)
                assertEquals(2, repository.requests)
                viewModel.watcher.setEnabled(false)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun watcherSubscriptionSurvivesViewModelRecreation() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val address = defaultThreadAddress(42L)
                val watches = FakeWatchRepository()
                val first =
                    ThreadViewModel(
                        address,
                        FakeThreadRepository(success(address)),
                        ThreadPersistence(FakeThreadLibrary(), watches),
                    )
                advanceUntilIdle()

                first.toggleWatcher()
                runCurrent()
                assertEquals(43L, watches.subscription?.lastKnownPostId?.value)
                first.watcher.setEnabled(false)

                val restored =
                    ThreadViewModel(
                        address,
                        FakeThreadRepository(success(address)),
                        ThreadPersistence(FakeThreadLibrary(), watches),
                    )
                runCurrent()

                assertEquals(true, restored.watcher.state.value.enabled)
                restored.watcher.setEnabled(false)
            } finally {
                Dispatchers.resetMain()
            }
        }

    private fun imageAttachment(): MediaAttachment =
        MediaAttachment(
            id = requireNotNull(AttachmentId.parse("cat")),
            fileName = "cat.jpg",
            source =
                AttachmentSource(
                    remoteUrl = requireNotNull(RemoteUrl.parse("https://2ch.su/src/cat.jpg")),
                    thumbnailUrl = requireNotNull(RemoteUrl.parse("https://2ch.su/thumb/cat.jpg")),
                ),
            byteSize = requireNotNull(ByteSize.parse(12L * 1024L)),
            kind = MediaKind.IMAGE,
            dimensions = MediaDimensions.create(1024, 768),
        )

    private fun videoAttachment(): MediaAttachment =
        MediaAttachment(
            id = requireNotNull(AttachmentId.parse("clip")),
            fileName = "clip.webm",
            source =
                AttachmentSource(
                    remoteUrl = requireNotNull(RemoteUrl.parse("https://2ch.su/src/clip.webm")),
                    thumbnailUrl = requireNotNull(RemoteUrl.parse("https://2ch.su/thumb/clip.jpg")),
                ),
            byteSize = requireNotNull(ByteSize.parse(1024L)),
            kind = MediaKind.VIDEO,
        )

    private fun success(
        address: ThreadAddress,
        postIds: List<Long> = listOf(43L),
    ): DomainOutcome<ThreadSnapshot> =
        DomainOutcome.Success(
            ThreadSnapshot(
                address = address,
                subject = "Тестовый тред",
                flags = ThreadFlags(pinned = false, closed = false, archived = false),
                posts =
                    postIds.map { postId ->
                        BoardPost(
                            address =
                                PostAddress(
                                    address,
                                    requireNotNull(PostId.parse(postId)),
                                ),
                            content =
                                PostContent(
                                    document = PostDocument.plain("Ответ"),
                                    metadata = PostMetadata(name = "Аноним", date = null, sage = false),
                                ),
                            attachments = listOf(imageAttachment(), videoAttachment()),
                        )
                    },
            ),
        )
}
