package com.solthariel.pawchan

import androidx.compose.runtime.Composable
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.postpreferences.PostPreferences
import com.solthariel.pawchan.domain.postpreferences.PostPreferencesRepository
import com.solthariel.pawchan.thread.PostContentAccess
import com.solthariel.pawchan.thread.PostMenuCommand
import com.solthariel.pawchan.thread.PostMenuEnvironment
import com.solthariel.pawchan.thread.PostMenuTransfer
import com.solthariel.pawchan.thread.ThreadActions
import com.solthariel.pawchan.thread.ThreadPostContext
import com.solthariel.pawchan.thread.ThreadPostPreferencesController
import com.solthariel.pawchan.thread.ThreadPostStorage
import com.solthariel.pawchan.thread.ThreadPostVisibility
import com.solthariel.pawchan.thread.ThreadPresentation
import com.solthariel.pawchan.thread.ThreadToolsState
import com.solthariel.pawchan.thread.executePostMenu
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadPostStorageBoundaryTest {
    @Test fun removedPostFavoritesHaveNoMenuCommand() {
        assertTrue(PostMenuCommand.entries.none { it.name.contains("FAVORITE") })
    }

    @Test
    fun ownMutationReachesStorageOnlyAfterPreferencesAreAccepted() =
        runTest {
            val storage = RecordingPostStorage()
            val post = toolsPost(7, "Текст")
            val content = toolsContent(listOf(post))
            val address = defaultThreadAddress(42)
            val preferences =
                ThreadPostPreferencesController(
                    address,
                    null,
                    PostContentAccess({ content }, {}),
                    StandardTestDispatcher(testScheduler),
                )
            val environment =
                PostMenuEnvironment(
                    ThreadToolsState(this),
                    ThreadActions({
                    }, {}, {}, {}),
                    ThreadPresentation(),
                    ThreadPostContext(address, preferences, storage),
                )
            executePostMenu(PostMenuCommand.OWN, post, environment, NoPostTransfer)
            assertTrue(storage.owned.isEmpty())
            assertEquals("Не удалось сохранить отметку или правило скрытия", environment.tools.notice)
            preferences.prepare(content)
            executePostMenu(PostMenuCommand.OWN, post, environment, NoPostTransfer)
            assertEquals(listOf(PostAddress(address, requireNotNull(PostId.parse(7))) to true), storage.owned)
        }

    @Test
    fun rejectedPreferenceSaveDoesNotDivergeFromWorkspaceOwnState() =
        runTest {
            val storage = RecordingPostStorage()
            val post = toolsPost(7, "Текст")
            val content = toolsContent(listOf(post))
            val address = defaultThreadAddress(42)
            val preferences =
                ThreadPostPreferencesController(
                    address,
                    RejectingOwnPreferences(),
                    PostContentAccess({
                        content
                    }, {}),
                    StandardTestDispatcher(testScheduler),
                )
            val environment =
                PostMenuEnvironment(
                    ThreadToolsState(this),
                    ThreadActions({
                    }, {}, {}, {}),
                    ThreadPresentation(),
                    ThreadPostContext(address, preferences, storage),
                )
            preferences.prepare(content)
            executePostMenu(PostMenuCommand.OWN, post, environment, NoPostTransfer)
            assertTrue(storage.owned.isEmpty())
            assertEquals("Не удалось сохранить отметку или правило скрытия", environment.tools.notice)
        }

    @Test
    fun invalidPostIdDoesNotInventAnAddressOrReachStorage() =
        runTest {
            val storage = RecordingPostStorage()
            val post = toolsPost(-7, "Текст")
            val content = toolsContent(listOf(post))
            val address = defaultThreadAddress(42)
            val preferences =
                ThreadPostPreferencesController(
                    address,
                    null,
                    PostContentAccess({ content }, {}),
                    StandardTestDispatcher(testScheduler),
                )
            val environment =
                PostMenuEnvironment(
                    ThreadToolsState(this),
                    ThreadActions({
                    }, {}, {}, {}),
                    ThreadPresentation(),
                    ThreadPostContext(address, preferences, storage),
                )
            preferences.prepare(content)
            executePostMenu(PostMenuCommand.OWN, post, environment, NoPostTransfer)
            assertTrue(storage.owned.isEmpty())
        }

    @Test
    fun absentStorageDoesNotBreakOwnCommand() =
        runTest {
            val post = toolsPost(7, "Текст")
            val content = toolsContent(listOf(post))
            val address = defaultThreadAddress(42)
            val preferences =
                ThreadPostPreferencesController(
                    address,
                    null,
                    PostContentAccess({ content }, {}),
                    StandardTestDispatcher(testScheduler),
                )
            val environment =
                PostMenuEnvironment(
                    ThreadToolsState(this),
                    ThreadActions({}, {}, {}, {}),
                    ThreadPresentation(),
                    ThreadPostContext(address, preferences),
                )
            preferences.prepare(content)
            executePostMenu(PostMenuCommand.OWN, post, environment, NoPostTransfer)
            assertTrue(environment.tools.notice == null)
        }

    @Test
    fun showAutoHiddenPostOnlyRevealsTheSession() =
        runTest {
            val storage = RecordingPostStorage()
            val revealed = mutableListOf<Long>()
            val original = toolsPost(7)
            val post =
                original.copy(
                    metadata = original.metadata.copy(visibility = ThreadPostVisibility(true, "Rule", "r")),
                )
            val content = toolsContent(listOf(post))
            val address = defaultThreadAddress(42)
            val preferences =
                ThreadPostPreferencesController(
                    address,
                    null,
                    PostContentAccess({ content }, {}),
                    StandardTestDispatcher(testScheduler),
                )
            val environment =
                PostMenuEnvironment(
                    ThreadToolsState(this),
                    ThreadActions({}, {}, {}, {}),
                    ThreadPresentation(),
                    ThreadPostContext(address, preferences, storage) { revealed += it },
                )
            executePostMenu(PostMenuCommand.SHOW, post, environment, NoPostTransfer)
            assertEquals(listOf(7L), revealed)
            assertNull(preferences.currentRules.value)
            assertTrue(storage.owned.isEmpty())
        }
}

private class RecordingPostStorage : ThreadPostStorage {
    val owned = mutableListOf<Pair<PostAddress, Boolean>>()

    override fun own(
        address: PostAddress,
        own: Boolean,
    ) {
        owned += address to own
    }
}

private object NoPostTransfer : PostMenuTransfer {
    override fun copy(text: String) {
        error("Unexpected copy")
    }
}

private class RejectingOwnPreferences : PostPreferencesRepository {
    override suspend fun loadPostPreferences(address: ThreadAddress): DomainOutcome<PostPreferences> =
        DomainOutcome.Success(PostPreferences())

    override suspend fun savePostPreferences(
        address: ThreadAddress,
        value: PostPreferences,
    ): DomainOutcome<Unit> = DomainOutcome.Failure(DomainFailure.StorageUnavailable)
}
