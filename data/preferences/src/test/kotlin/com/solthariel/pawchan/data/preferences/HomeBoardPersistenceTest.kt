package com.solthariel.pawchan.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.settings.ThemePreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class HomeBoardPersistenceTest {
    @get:Rule
    val temporary = TemporaryFolder()
    private val board = BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("biz")))

    @Test
    fun homeBoardSurvivesReopeningTheStore() =
        runBlocking {
            val file = File(temporary.root, "settings.preferences_pb")
            val first = repository(file)
            try {
                assertEquals(DomainOutcome.Success(Unit), first.first.setHomeBoard(board))
            } finally {
                first.second.cancelAndJoin()
            }
            val reopened = repository(file)
            try {
                assertEquals(board, reopened.first.homeBoard.first())
            } finally {
                reopened.second.cancelAndJoin()
            }
        }

    @Test
    fun changingHomeDoesNotOverwriteThemeOrFavoriteBoards() =
        runBlocking {
            val repository = DataStoreSettingsRepository(InMemoryPreferencesDataStore())

            assertEquals(DomainOutcome.Success(Unit), repository.setTheme(ThemePreference.AMOLED))
            assertEquals(DomainOutcome.Success(Unit), repository.setFavoriteBoard(board, true))
            assertEquals(DomainOutcome.Success(Unit), repository.setHomeBoard(board))

            assertEquals(ThemePreference.AMOLED, repository.theme.first())
            assertEquals(setOf(board), repository.favoriteBoards.first())
            assertEquals(board, repository.homeBoard.first())
        }

    @Test
    fun favoriteBoardsKeepSitesDistinctAndExportBothFormats() =
        runBlocking {
            val repository = DataStoreSettingsRepository(InMemoryPreferencesDataStore())
            repository.portable.replace(mapOf("favorite_boards_v2" to "biz"), null)
            val other = board.copy(site = requireNotNull(SiteId.parse("4chan")))
            assertEquals(setOf(board), repository.favoriteBoards.first())
            assertEquals(DomainOutcome.Success(Unit), repository.setFavoriteBoard(other, true))
            assertEquals(setOf(board, other), repository.favoriteBoards.first())
            val saved = repository.portable.export()
            assertEquals("biz", saved["favorite_boards_v2"])
            assertEquals("4chan/biz", saved["favorite_boards_other_v1"])
            repository.setFavoriteBoard(board, false)
            assertEquals(setOf(other), repository.favoriteBoards.first())
            repository.portable.replace(saved, null)
            assertEquals(setOf(board, other), repository.favoriteBoards.first())
        }

    @Test
    fun favoritesSurviveReopeningAndRemainIndependent() =
        runBlocking {
            val file = File(temporary.root, "favorites.preferences_pb")
            val other = board.copy(site = requireNotNull(SiteId.parse("4chan")))
            val first = repository(file)
            try {
                assertEquals(DomainOutcome.Success(Unit), first.first.setFavoriteBoard(board, true))
                assertEquals(DomainOutcome.Success(Unit), first.first.setFavoriteBoard(other, true))
                assertEquals("4chan/biz", first.first.portable.export()["favorite_boards_other_v1"])
            } finally {
                first.second.cancelAndJoin()
            }
            val reopened = repository(file)
            try {
                assertEquals(setOf(board, other), reopened.first.favoriteBoards.first())
                reopened.first.setFavoriteBoard(board, false)
                assertEquals(setOf(other), reopened.first.favoriteBoards.first())
            } finally {
                reopened.second.cancelAndJoin()
            }
        }

    @Test
    fun changingFavoritesPreservesUnrecognizedStoredValues() =
        runBlocking {
            val store = InMemoryPreferencesDataStore()
            val oldKey = stringSetPreferencesKey("favorite_boards")
            val otherKey = stringPreferencesKey("favorite_boards_other_v1")
            store.updateData { it.toMutablePreferences().apply { this[oldKey] = setOf("biz", "invalid/legacy") } }
            val repository = DataStoreSettingsRepository(store)
            val other = board.copy(site = requireNotNull(SiteId.parse("4chan")))
            repository.setFavoriteBoard(other, true)
            assertEquals(setOf("biz", "invalid/legacy"), store.data.first()[oldKey])
            store.updateData { it.toMutablePreferences().apply { this[otherKey] = "4chan/biz\nfuture/invalid/board" } }
            repository.setFavoriteBoard(board, false)
            assertEquals("invalid/legacy", store.data.first()[stringPreferencesKey("favorite_boards_v2")])
            assertEquals("4chan/biz\nfuture/invalid/board", store.data.first()[otherKey])
            repository.setFavoriteBoard(other, false)
            assertEquals("future/invalid/board", store.data.first()[otherKey])
        }

    @Test
    fun changingAmoledToLightOrDarkDisablesBlackWithoutLosingPalette() =
        runBlocking {
            val repository = DataStoreSettingsRepository(InMemoryPreferencesDataStore())
            repository.portable.replace(mapOf("theme" to "AMOLED"), null)
            repository.setTheme(ThemePreference.LIGHT)
            assertEquals(
                com.solthariel.pawchan.domain.settings
                    .ThemeColors(pureBlack = false),
                repository.themeColors.first(),
            )
            repository.setTheme(ThemePreference.DARK)
            assertEquals(false, repository.themeColors.first().pureBlack)
        }

    @Test
    fun colorSelectionSurvivesReopeningTheStore() =
        runBlocking {
            val file = File(temporary.root, "colors.preferences_pb")
            val colors =
                com.solthariel.pawchan.domain.settings.ThemeColors(
                    com.solthariel.pawchan.domain.settings.ThemePalette.AMBER,
                    pureBlack = true,
                )
            val first = repository(file)
            try {
                assertEquals(DomainOutcome.Success(Unit), first.first.setThemeColors(colors))
                first.first.setTheme(ThemePreference.AMOLED)
            } finally {
                first.second.cancelAndJoin()
            }
            val reopened = repository(file)
            try {
                assertEquals(colors, reopened.first.themeColors.first())
                assertEquals(ThemePreference.AMOLED, reopened.first.theme.first())
            } finally {
                reopened.second.cancelAndJoin()
            }
        }

    private fun repository(file: File): Pair<DataStoreSettingsRepository, Job> {
        val job = SupervisorJob()
        val data = diskPreferencesTestStore(file, CoroutineScope(job + Dispatchers.IO))
        return DataStoreSettingsRepository(data) to job
    }

    private class InMemoryPreferencesDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow<Preferences>(emptyPreferences())

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }
}
