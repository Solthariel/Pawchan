package com.solthariel.pawchan.data.preferences

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.solthariel.pawchan.domain.settings.NavigationPreferences

internal fun Preferences.readNavigation(): NavigationPreferences =
    NavigationPreferences(
        this[PAGES] ?: NavigationPreferences().pages,
        this[FAVORITES] ?: NavigationPreferences().favorites,
        this[REPLIES] ?: NavigationPreferences().replies,
    ).normalized()

internal fun MutablePreferences.writeNavigation(value: NavigationPreferences) {
    val normalized = value.normalized()
    this[PAGES] = normalized.pages
    this[FAVORITES] = normalized.favorites
    this[REPLIES] = normalized.replies
}

private val PAGES = intPreferencesKey("navigation_pages_limit")
private val FAVORITES = intPreferencesKey("navigation_favorites_limit")
private val REPLIES = intPreferencesKey("navigation_replies_limit")
