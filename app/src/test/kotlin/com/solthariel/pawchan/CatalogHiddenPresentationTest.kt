package com.solthariel.pawchan

import com.solthariel.pawchan.catalog.CatalogUiState
import com.solthariel.pawchan.catalog.CatalogViewModel
import com.solthariel.pawchan.domain.catalog.CatalogRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardDescriptor
import com.solthariel.pawchan.domain.model.CatalogSnapshot
import com.solthariel.pawchan.domain.model.CatalogThread
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.model.ThreadStatistics
import com.solthariel.pawchan.navigation.defaultBoardAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogHiddenPresentationTest {
    @Test
    fun hiddenStateKeepsRowIdentityAndSurvivesRefreshThenRestores() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val address = ThreadAddress(defaultBoardAddress(), requireNotNull(ThreadId.parse(42)))
                val hidden = MutableStateFlow(setOf(address))
                val viewModel =
                    CatalogViewModel(
                        Repository(address),
                        uiDispatcher = dispatcher,
                        hidden = hidden,
                        placeholders = MutableStateFlow(true),
                    )
                advanceUntilIdle()
                val initial = (viewModel.state.value as CatalogUiState.Content).threads.single()
                assertEquals(address, initial.address)
                assertEquals(true, initial.hidden)
                viewModel.refresh()
                advanceUntilIdle()
                assertEquals(true, (viewModel.state.value as CatalogUiState.Content).threads.single().hidden)
                hidden.value = emptySet()
                advanceUntilIdle()
                assertEquals(
                    initial.copy(hidden = false),
                    (viewModel.state.value as CatalogUiState.Content).threads.single(),
                )
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun autoPlaceholderCanBeCompletelyRemovedAndTemporarilyRevealedWithoutChangingTheRule() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val address = ThreadAddress(defaultBoardAddress(), requireNotNull(ThreadId.parse(42)))
                val rule =
                    com.solthariel.pawchan.settings
                        .newContentFilterRule(address.board, "OP")
                val show = MutableStateFlow(true)
                val model =
                    CatalogViewModel(
                        Repository(address),
                        uiDispatcher = dispatcher,
                        rules = MutableStateFlow(listOf(rule)),
                        placeholders = show,
                    )
                advanceUntilIdle()
                val collapsed = (model.state.value as CatalogUiState.Content).threads.single()
                assertEquals(true, collapsed.hidden)
                assertEquals("OP", collapsed.flags.autoHide?.label)
                assertEquals(rule.id, collapsed.flags.autoHide?.ruleId)
                assertEquals(emptyMap<Long, Any>(), (model.state.value as CatalogUiState.Content).viewerMediaGroups)
                show.value = false
                advanceUntilIdle()
                assertEquals(0, (model.state.value as CatalogUiState.Content).threads.size)
                assertEquals(1, (model.state.value as CatalogUiState.Content).autoHiddenThreads.size)
                model.toggleFilteredThreads()
                advanceUntilIdle()
                val revealed = (model.state.value as CatalogUiState.Content).threads.single()
                assertEquals(false, revealed.hidden)
                assertEquals(null, revealed.flags.autoHide)
                assertEquals(collapsed.statistics, revealed.statistics)
                assertEquals(true, rule.enabled)
            } finally {
                Dispatchers.resetMain()
            }
        }

    private class Repository(
        private val address: ThreadAddress,
    ) : CatalogRepository {
        override suspend fun loadCatalog(board: BoardAddress): DomainOutcome<CatalogSnapshot> =
            DomainOutcome.Success(
                CatalogSnapshot(
                    BoardDescriptor(board, "Бред", false),
                    listOf(
                        CatalogThread(
                            address = address,
                            subject = "Title",
                            content = PostContent(PostDocument.plain("OP"), PostMetadata(null, null, false)),
                            attachments = emptyList(),
                            statistics = requireNotNull(ThreadStatistics.create(0, 0, 0)),
                            flags = ThreadFlags(false, false, false),
                        ),
                    ),
                    0,
                ),
            )
    }
}
