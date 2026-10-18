package com.solthariel.pawchan.data.preferences

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.solthariel.pawchan.domain.settings.AutomationPreferences
import com.solthariel.pawchan.domain.settings.AutomationPreferences.Companion.DEFAULT_OPEN_SECONDS
import com.solthariel.pawchan.domain.settings.AutomationPreferences.Companion.MIN_BACKGROUND_MINUTES
import com.solthariel.pawchan.domain.settings.DownloadNaming
import com.solthariel.pawchan.domain.settings.DownloadPreferences
import com.solthariel.pawchan.domain.settings.NotificationPreferences
import com.solthariel.pawchan.domain.settings.StoragePreferences
import com.solthariel.pawchan.domain.settings.StoragePreferences.Companion.DEFAULT_MB

internal fun Preferences.readAutomation() =
    AutomationPreferences(
        openThread = this[OPEN] ?: false,
        openIntervalSeconds = this[OPEN_INTERVAL]?.takeIf { it in OPEN_INTERVALS } ?: DEFAULT_OPEN_SECONDS,
        favorites = this[FAVORITES] ?: true,
        replies = this[REPLIES] ?: true,
        backgroundMinutes = this[BACKGROUND]?.takeIf { it in BACKGROUND_INTERVALS } ?: MIN_BACKGROUND_MINUTES,
        wifiOnly = this[WIFI] ?: false,
        notifications =
            NotificationPreferences(
                replies = this[NOTIFY_REPLIES] ?: true,
                posts = this[NOTIFY_POSTS] ?: true,
            ),
    )

internal fun MutablePreferences.writeAutomation(value: AutomationPreferences) {
    this[OPEN] = value.openThread
    this[OPEN_INTERVAL] = value.openIntervalSeconds.takeIf { it in OPEN_INTERVALS } ?: DEFAULT_OPEN_SECONDS
    this[FAVORITES] = value.favorites
    this[REPLIES] = value.replies
    this[BACKGROUND] = value.backgroundMinutes.takeIf { it in BACKGROUND_INTERVALS } ?: MIN_BACKGROUND_MINUTES
    this[WIFI] = value.wifiOnly
    this[NOTIFY_REPLIES] = value.notifications.replies
    this[NOTIFY_POSTS] = value.notifications.posts
}

internal fun Preferences.readDownloads() =
    DownloadPreferences(
        treeUri = this[TREE]?.takeIf { it.startsWith("content://") },
        naming = DownloadNaming.entries.firstOrNull { it.name == this[NAMING] } ?: DownloadNaming.ORIGINAL,
    )

internal fun MutablePreferences.writeDownloads(value: DownloadPreferences) {
    if (value.treeUri == null) remove(TREE) else this[TREE] = value.treeUri.orEmpty()
    this[NAMING] = value.naming.name
}

internal fun Preferences.readStorage(): StoragePreferences {
    val range = StoragePreferences.MIN_MB..StoragePreferences.MAX_MB
    val size = (this[CACHE] ?: DEFAULT_MB).takeIf { it in range } ?: DEFAULT_MB
    return StoragePreferences(size)
}

internal fun MutablePreferences.writeStorage(value: StoragePreferences) {
    this[CACHE] = value.cacheMb.takeIf { it in StoragePreferences.MIN_MB..StoragePreferences.MAX_MB } ?: DEFAULT_MB
}

private val OPEN = booleanPreferencesKey("auto_open_thread")
private val OPEN_INTERVAL = intPreferencesKey("auto_open_seconds")
private val FAVORITES = booleanPreferencesKey("auto_favorites")
private val REPLIES = booleanPreferencesKey("auto_replies")
private val BACKGROUND = intPreferencesKey("auto_background_minutes")
private val WIFI = booleanPreferencesKey("auto_wifi_only")
private val NOTIFY_REPLIES = booleanPreferencesKey("auto_notify_replies")
private val NOTIFY_POSTS = booleanPreferencesKey("auto_notify_posts")
private val TREE = stringPreferencesKey("download_tree")
private val NAMING = stringPreferencesKey("download_naming")
private val CACHE = intPreferencesKey("storage_cache_mb")
private val OPEN_INTERVALS = AutomationPreferences.OPEN_INTERVALS
private val BACKGROUND_INTERVALS = AutomationPreferences.BACKGROUND_INTERVALS
