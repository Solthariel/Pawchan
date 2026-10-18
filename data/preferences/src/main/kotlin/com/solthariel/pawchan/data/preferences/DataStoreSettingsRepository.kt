package com.solthariel.pawchan.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.solthariel.pawchan.data.maintenance.MaintenanceGate
import com.solthariel.pawchan.domain.boards.BoardPreferencesRepository
import com.solthariel.pawchan.domain.boards.FAVORITES_BOARD_CATEGORY_KEY
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.postpreferences.PostPreferencesRepository
import com.solthariel.pawchan.domain.settings.AppSettingsRepository
import com.solthariel.pawchan.domain.settings.SettingsRepository
import com.solthariel.pawchan.domain.settings.ThemePreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.IOException

class DataStoreSettingsRepository private constructor(
    private val dataStore: DataStore<Preferences>,
    private val scope: CoroutineScope?,
) : SettingsRepository,
    BoardPreferencesRepository,
    PostPreferencesRepository by PostPreferencesStore(dataStore),
    AppSettingsRepository by AppSettingsStore(dataStore),
    AutoCloseable {
    val usage: com.solthariel.pawchan.domain.statistics.UsageStatisticsRepository = UsageStatisticsStore(dataStore)

    val notificationPrompt = NotificationPromptStore(dataStore)
    val portable: PortableSettings =
        DataStorePortableSettings(dataStore) {
            themeOverride.value = null
            homeBoardOverride.value = null
            homeBoardWritten.value = false
            favoriteBoardsOverride.value = null
        }

    private val themeOverride = MutableStateFlow<ThemePreference?>(null)
    private val homeBoardOverride = MutableStateFlow<BoardAddress?>(null)
    private val homeBoardWritten = MutableStateFlow(false)
    private val favoriteBoardsOverride = MutableStateFlow<Set<BoardId>?>(null)

    override val theme: Flow<ThemePreference> =
        combine(dataStore.safeData(), themeOverride) { preferences, override ->
            override ?: parseTheme(preferences[THEME_KEY])
        }

    override val homeBoard: Flow<BoardAddress?> =
        combine(dataStore.safeData(), homeBoardOverride, homeBoardWritten) { preferences, override, written ->
            if (written) override else parseHomeBoard(preferences[HOME_BOARD_KEY])
        }

    override suspend fun setHomeBoard(board: BoardAddress): DomainOutcome<Unit> {
        val outcome = editPreferences { it[HOME_BOARD_KEY] = "${board.site.value}/${board.board.value}" }
        if (outcome is DomainOutcome.Success) {
            homeBoardOverride.value = board
            homeBoardWritten.value = true
        }
        return outcome
    }

    override val favoriteBoards: Flow<Set<BoardId>> =
        combine(dataStore.safeData(), favoriteBoardsOverride) { preferences, override ->
            override ?: parseFavoriteBoards(preferences.favoriteBoardValues())
        }

    override val expandedBoardCategories: Flow<Set<String>> =
        dataStore.safeData().map(Preferences::expandedBoardCategoryValues)

    override suspend fun setTheme(theme: ThemePreference): DomainOutcome<Unit> {
        val outcome =
            editPreferences { preferences ->
                preferences[THEME_KEY] = theme.name
            }
        if (outcome is DomainOutcome.Success) {
            themeOverride.value = theme
        }
        return outcome
    }

    override suspend fun setFavoriteBoard(
        board: BoardId,
        favorite: Boolean,
    ): DomainOutcome<Unit> {
        var updatedBoards = emptySet<String>()
        val outcome =
            editPreferences { preferences ->
                val boards = preferences.favoriteBoardValues()
                updatedBoards =
                    if (favorite) {
                        boards + board.value
                    } else {
                        boards - board.value
                    }
                if (updatedBoards.isEmpty()) {
                    preferences.remove(FAVORITE_BOARDS_KEY)
                } else {
                    preferences[FAVORITE_BOARDS_KEY] =
                        updatedBoards.sorted().joinToString(FAVORITE_BOARD_SEPARATOR)
                }
                preferences.remove(LEGACY_FAVORITE_BOARDS_KEY)
            }
        if (outcome is DomainOutcome.Success) {
            favoriteBoardsOverride.value = parseFavoriteBoards(updatedBoards)
        }
        return outcome
    }

    override suspend fun setBoardCategoryExpanded(
        category: String,
        expanded: Boolean,
    ): DomainOutcome<Unit> =
        editPreferences { preferences ->
            val current = preferences.expandedBoardCategoryValues()
            val updated = if (expanded) current + category else current - category
            preferences[EXPANDED_BOARD_CATEGORIES_KEY] =
                updated.sorted().joinToString(EXPANDED_BOARD_CATEGORY_SEPARATOR)
        }

    override fun close() {
        scope?.cancel()
    }

    private suspend fun editPreferences(transform: suspend (MutablePreferences) -> Unit): DomainOutcome<Unit> =
        try {
            dataStore.edit(transform)
            DomainOutcome.Success(Unit)
        } catch (_: IOException) {
            DomainOutcome.Failure(DomainFailure.StorageUnavailable)
        }

    internal constructor(dataStore: DataStore<Preferences>) : this(dataStore, scope = null)

    companion object {
        fun create(context: Context): DataStoreSettingsRepository {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val dataStore =
                PreferenceDataStoreFactory.create(scope = scope) {
                    context.applicationContext.preferencesDataStoreFile(DATASTORE_FILE)
                }
            return DataStoreSettingsRepository(
                MaintainedDataStore(dataStore, MaintenanceGate(File(context.filesDir, "backup-maintenance"))),
                scope,
            )
        }
    }
}

private fun DataStore<Preferences>.safeData(): Flow<Preferences> =
    data.catch { error ->
        if (error is IOException) {
            emit(emptyPreferences())
        } else {
            throw error
        }
    }

private fun Preferences.favoriteBoardValues(): Set<String> {
    val encoded = this[FAVORITE_BOARDS_KEY]
    return if (encoded == null) {
        this[LEGACY_FAVORITE_BOARDS_KEY].orEmpty()
    } else {
        encoded
            .split(FAVORITE_BOARD_SEPARATOR)
            .filterTo(linkedSetOf(), String::isNotBlank)
    }
}

private fun Preferences.expandedBoardCategoryValues(): Set<String> {
    val encoded = this[EXPANDED_BOARD_CATEGORIES_KEY] ?: return setOf(FAVORITES_BOARD_CATEGORY_KEY)
    return encoded
        .split(EXPANDED_BOARD_CATEGORY_SEPARATOR)
        .filterTo(linkedSetOf(), String::isNotBlank)
}

private val HOME_BOARD_KEY = stringPreferencesKey("home_board")
private val THEME_KEY = stringPreferencesKey("theme")
private val FAVORITE_BOARDS_KEY = stringPreferencesKey("favorite_boards_v2")
private val LEGACY_FAVORITE_BOARDS_KEY = stringSetPreferencesKey("favorite_boards")
private val EXPANDED_BOARD_CATEGORIES_KEY = stringPreferencesKey("expanded_board_categories")
private const val FAVORITE_BOARD_SEPARATOR = "\n"
private const val EXPANDED_BOARD_CATEGORY_SEPARATOR = "\n"
private const val DATASTORE_FILE = "settings.preferences_pb"
