package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.postpreferences.PostPreferences
import com.solthariel.pawchan.domain.postpreferences.PostPreferencesRepository
import com.solthariel.pawchan.thread.PostContentAccess
import com.solthariel.pawchan.thread.PostPreferenceAction
import com.solthariel.pawchan.thread.ThreadPostPreferencesController
import com.solthariel.pawchan.thread.ThreadUiState
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadPostPreferencesTest {
    private val address = ThreadAddress(defaultBoardAddress(), requireNotNull(ThreadId.parse(42)))

    @Test
    fun markingAndHidingPersistAndAreAppliedAfterAControllerIsRecreated() =
        runTest {
            val store = TestPostPreferencesRepository()
            var state: ThreadUiState = toolsContent(listOf(toolsPost(1), toolsPost(2)))
            val access = PostContentAccess({ state }, { state = it })
            val dispatcher = StandardTestDispatcher(testScheduler)
            val controller = ThreadPostPreferencesController(address, store, access, dispatcher)
            state = controller.prepare(state)
            assertTrue(controller.change(PostPreferenceAction.OWN, (state as ThreadUiState.Content).posts[0]))
            assertTrue(controller.change(PostPreferenceAction.HIDE, (state as ThreadUiState.Content).posts[1]))
            val restored =
                ThreadPostPreferencesController(address, store, access, dispatcher)
                    .prepare(state) as ThreadUiState.Content
            assertTrue(
                restored.posts[0]
                    .metadata.changes.own,
            )
            assertTrue(restored.posts[1].metadata.hidden)
            assertEquals(1, restored.presentation.postPositions[2])
        }

    @Test
    fun failedSavingDoesNotChangeThePublishedPost() =
        runTest {
            val store = TestPostPreferencesRepository()
            var state: ThreadUiState = toolsContent(listOf(toolsPost(1)))
            val controller =
                ThreadPostPreferencesController(
                    address,
                    store,
                    PostContentAccess({ state }, { state = it }),
                    StandardTestDispatcher(testScheduler),
                )
            state = controller.prepare(state)
            store.failWrite = true
            assertFalse(controller.change(PostPreferenceAction.HIDE, (state as ThreadUiState.Content).posts[0]))
            assertFalse((state as ThreadUiState.Content).posts[0].metadata.hidden)
            assertEquals(PostPreferences(), store.value)
        }

    @Test
    fun failedReadingCannotOverwriteExistingRulesWithEmptyDefaults() =
        runTest {
            val store = TestPostPreferencesRepository()
            store.value = PostPreferences(hidden = setOf(1))
            store.failRead = true
            var state: ThreadUiState = toolsContent(listOf(toolsPost(1)))
            val controller =
                ThreadPostPreferencesController(
                    address,
                    store,
                    PostContentAccess({ state }, { state = it }),
                    StandardTestDispatcher(testScheduler),
                )
            state = controller.prepare(state)
            assertFalse(controller.change(PostPreferenceAction.OWN, (state as ThreadUiState.Content).posts[0]))
            assertEquals(setOf(1L), store.value.hidden)
        }
}

internal class TestPostPreferencesRepository : PostPreferencesRepository {
    var value = PostPreferences()
    var failRead = false
    var failWrite = false

    override suspend fun loadPostPreferences(address: ThreadAddress): DomainOutcome<PostPreferences> =
        if (failRead) DomainOutcome.Failure(DomainFailure.StorageUnavailable) else DomainOutcome.Success(value)

    override suspend fun savePostPreferences(
        address: ThreadAddress,
        value: PostPreferences,
    ): DomainOutcome<Unit> {
        if (failWrite) return DomainOutcome.Failure(DomainFailure.StorageUnavailable)
        this.value = value
        return DomainOutcome.Success(Unit)
    }
}
