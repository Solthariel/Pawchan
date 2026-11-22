package com.solthariel.pawchan.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.settings.AppBehavior
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.settings.AppSettingsRepository
import com.solthariel.pawchan.domain.settings.AutohidePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Глобальные настройки в том же DataStore, что тема и стартовая доска. */
internal class AppSettingsStore(
    private val dataStore: DataStore<Preferences>,
) : AppSettingsRepository {
    override val appSettings: Flow<AppSettings> =
        dataStore.data
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .map(Preferences::toAppSettings)

    override suspend fun updateAppSettings(transform: (AppSettings) -> AppSettings): DomainOutcome<Unit> =
        try {
            dataStore.edit { preferences -> preferences.write(transform(preferences.toAppSettings())) }
            DomainOutcome.Success(Unit)
        } catch (_: IOException) {
            DomainOutcome.Failure(DomainFailure.StorageUnavailable)
        }
}

internal fun Preferences.toAppSettings(): AppSettings {
    val defaults = AppSettings()
    return AppSettings(
        postMarkers = this[POST_MARKERS] ?: defaults.postMarkers,
        hideStickers = this[HIDE_STICKERS] ?: defaults.hideStickers,
        hideMonkeyReplies = this[HIDE_MONKEY_REPLIES] ?: defaults.hideMonkeyReplies,
        showDefaultName = this[DEFAULT_NAME] ?: defaults.showDefaultName,
        textSize =
            (this[TEXT_SIZE] ?: defaults.textSize).coerceIn(AppSettings.MIN_TEXT_SIZE, AppSettings.MAX_TEXT_SIZE),
        collapseLines =
            (this[COLLAPSE_LINES] ?: defaults.collapseLines)
                .coerceIn(AppSettings.MIN_COLLAPSE_LINES, AppSettings.NO_COLLAPSE),
        behavior =
            AppBehavior(
                postCountEnabled = this[POST_COUNT] ?: defaults.postCountEnabled,
                client = readClientPreferences(),
                autohide =
                    AutohidePreferences(
                        decodeContentFilters(this[CONTENT_FILTER_RECORDS].orEmpty()),
                        this[CONTENT_FILTER_RESET_NOTICE] ?: false,
                    ),
                showHiddenThreads = this[SHOW_HIDDEN_THREADS] ?: defaults.behavior.showHiddenThreads,
                videoStartMuted = this[VIDEO_MUTED] ?: defaults.behavior.videoStartMuted,
                updateSavedCopies = this[UPDATE_COPIES] ?: defaults.behavior.updateSavedCopies,
            ),
    )
}

internal fun MutablePreferences.write(settings: AppSettings) {
    writeClientPreferences(settings.behavior.client)
    this[CONTENT_FILTER_RECORDS] = encodeContentFilters(settings.contentFilters)
    this[CONTENT_FILTER_RESET_NOTICE] = settings.behavior.contentFilterResetNotice
    this[SHOW_HIDDEN_THREADS] = settings.behavior.showHiddenThreads
    this[POST_MARKERS] = settings.postMarkers
    this[HIDE_STICKERS] = settings.hideStickers
    this[HIDE_MONKEY_REPLIES] = settings.hideMonkeyReplies
    this[POST_COUNT] = settings.postCountEnabled
    this[DEFAULT_NAME] = settings.showDefaultName
    this[TEXT_SIZE] = settings.textSize
    this[COLLAPSE_LINES] = settings.collapseLines
    this[VIDEO_MUTED] = settings.behavior.videoStartMuted
    this[UPDATE_COPIES] = settings.behavior.updateSavedCopies
}

private val HIDE_MONKEY_REPLIES = booleanPreferencesKey("app_hide_dvach_monkey_replies")
private val HIDE_STICKERS = booleanPreferencesKey("app_hide_stickers")
private val POST_COUNT = booleanPreferencesKey("app_postcount_enabled")
private val POST_MARKERS = booleanPreferencesKey("app_post_markers")
private val DEFAULT_NAME = booleanPreferencesKey("app_show_default_name")
private val TEXT_SIZE = intPreferencesKey("app_text_size")
private val COLLAPSE_LINES = intPreferencesKey("app_collapse_lines")
private val VIDEO_MUTED = booleanPreferencesKey("app_video_start_muted")
private val UPDATE_COPIES = booleanPreferencesKey("app_update_saved_copies")
private val SHOW_HIDDEN_THREADS = booleanPreferencesKey("app_show_hidden_threads")
