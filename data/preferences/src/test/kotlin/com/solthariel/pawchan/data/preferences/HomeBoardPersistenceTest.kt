package com.solthariel.pawchan.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
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
            assertEquals(DomainOutcome.Success(Unit), repository.setFavoriteBoard(board.board, true))
            assertEquals(DomainOutcome.Success(Unit), repository.setHomeBoard(board))

            assertEquals(ThemePreference.AMOLED, repository.theme.first())
            assertEquals(setOf(board.board), repository.favoriteBoards.first())
            assertEquals(board, repository.homeBoard.first())
        }

    private fun repository(file: File): Pair<DataStoreSettingsRepository, Job> {
        val job = SupervisorJob()
        val data = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { file }
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
