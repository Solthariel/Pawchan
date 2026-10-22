package com.solthariel.pawchan.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.settings.AppBehavior
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.settings.AppSettingsRepository
import com.solthariel.pawchan.domain.settings.DnsProvider
import com.solthariel.pawchan.domain.settings.ServiceDns
import com.solthariel.pawchan.domain.settings.ServiceDnsPreferences
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
        bubblePosts = this[BUBBLE_POSTS] ?: defaults.bubblePosts,
        hideStickers = this[HIDE_STICKERS] ?: defaults.hideStickers,
        showDefaultName = this[DEFAULT_NAME] ?: defaults.showDefaultName,
        textSize =
            (this[TEXT_SIZE] ?: defaults.textSize).coerceIn(AppSettings.MIN_TEXT_SIZE, AppSettings.MAX_TEXT_SIZE),
        collapseLines =
            (this[COLLAPSE_LINES] ?: defaults.collapseLines)
                .coerceIn(AppSettings.MIN_COLLAPSE_LINES, AppSettings.MAX_COLLAPSE_LINES),
        behavior =
            AppBehavior(
                postCountEnabled = this[POST_COUNT] ?: defaults.postCountEnabled,
                client = readClientPreferences(),
                contentFilters = decodeContentFilters(this[CONTENT_FILTERS].orEmpty()),
                showHiddenThreads = this[SHOW_HIDDEN_THREADS] ?: defaults.behavior.showHiddenThreads,
                videoStartMuted = this[VIDEO_MUTED] ?: defaults.behavior.videoStartMuted,
                updateSavedCopies = this[UPDATE_COPIES] ?: defaults.behavior.updateSavedCopies,
                dns = readServiceDns(defaults.behavior.dns),
            ),
    )
}

internal fun MutablePreferences.write(settings: AppSettings) {
    writeClientPreferences(settings.behavior.client)
    this[CONTENT_FILTERS] = encodeContentFilters(settings.contentFilters)
    this[SHOW_HIDDEN_THREADS] = settings.behavior.showHiddenThreads
    this[POST_MARKERS] = settings.postMarkers
    this[BUBBLE_POSTS] = settings.bubblePosts
    this[HIDE_STICKERS] = settings.hideStickers
    this[POST_COUNT] = settings.postCountEnabled
    this[DEFAULT_NAME] = settings.showDefaultName
    this[TEXT_SIZE] = settings.textSize
    this[COLLAPSE_LINES] = settings.collapseLines
    this[VIDEO_MUTED] = settings.behavior.videoStartMuted
    this[UPDATE_COPIES] = settings.behavior.updateSavedCopies
    this[GITHUB_DNS] = settings.behavior.dns.github.provider.name
    this[GITHUB_DNS_URL] = settings.behavior.dns.github.customUrl
    this[ARHIVACH_DNS] = settings.behavior.dns.arhivach.provider.name
    this[ARHIVACH_DNS_URL] = settings.behavior.dns.arhivach.customUrl
    this[VOCAROO_DNS] = settings.behavior.dns.vocaroo.provider.name
    this[VOCAROO_DNS_URL] = settings.behavior.dns.vocaroo.customUrl
}

private val CONTENT_FILTERS = stringPreferencesKey("app_content_filters_v1")
private val BUBBLE_POSTS = booleanPreferencesKey("app_bubble_posts")
private val HIDE_STICKERS = booleanPreferencesKey("app_hide_stickers")
private val POST_COUNT = booleanPreferencesKey("app_postcount_enabled")
private val POST_MARKERS = booleanPreferencesKey("app_post_markers")
private val DEFAULT_NAME = booleanPreferencesKey("app_show_default_name")
private val TEXT_SIZE = intPreferencesKey("app_text_size")
private val COLLAPSE_LINES = intPreferencesKey("app_collapse_lines")
private val VIDEO_MUTED = booleanPreferencesKey("app_video_start_muted")
private val UPDATE_COPIES = booleanPreferencesKey("app_update_saved_copies")
private val VOCAROO_DNS = stringPreferencesKey("app_vocaroo_dns")
private val VOCAROO_DNS_URL = stringPreferencesKey("app_vocaroo_dns_url")
private val SHOW_HIDDEN_THREADS = booleanPreferencesKey("app_show_hidden_threads")

private val GITHUB_DNS = stringPreferencesKey("app_github_dns")
private val GITHUB_DNS_URL = stringPreferencesKey("app_github_dns_url")
private val ARHIVACH_DNS = stringPreferencesKey("app_arhivach_dns")
private val ARHIVACH_DNS_URL = stringPreferencesKey("app_arhivach_dns_url")

private fun Preferences.readServiceDns(defaults: ServiceDnsPreferences): ServiceDnsPreferences =
    ServiceDnsPreferences(
        vocaroo = readDnsProfile(VOCAROO_DNS, VOCAROO_DNS_URL, defaults.vocaroo),
        arhivach = readDnsProfile(ARHIVACH_DNS, ARHIVACH_DNS_URL, defaults.arhivach),
        github = readDnsProfile(GITHUB_DNS, GITHUB_DNS_URL, defaults.github),
    )

private fun Preferences.readDnsProfile(
    providerKey: Preferences.Key<String>,
    urlKey: Preferences.Key<String>,
    defaults: ServiceDns,
): ServiceDns {
    val provider = DnsProvider.entries.firstOrNull { it.name == this[providerKey] } ?: defaults.provider
    return ServiceDns(provider, this[urlKey] ?: defaults.customUrl)
}
