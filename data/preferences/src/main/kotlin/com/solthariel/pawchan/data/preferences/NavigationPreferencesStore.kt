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
        this[BOTTOM_BAR_TRANSPARENCY] ?: NavigationPreferences.DEFAULT_BOTTOM_BAR_TRANSPARENCY,
        this[TOP_BAR_TRANSPARENCY] ?: NavigationPreferences.DEFAULT_TOP_BAR_TRANSPARENCY,
    ).normalized()

internal fun MutablePreferences.writeNavigation(value: NavigationPreferences) {
    val normalized = value.normalized()
    this[PAGES] = normalized.pages
    this[FAVORITES] = normalized.favorites
    this[REPLIES] = normalized.replies
    this[BOTTOM_BAR_TRANSPARENCY] = normalized.bottomBarTransparencyPercent
    this[TOP_BAR_TRANSPARENCY] = normalized.topBarTransparencyPercent
}

private val PAGES = intPreferencesKey("navigation_pages_limit")
private val FAVORITES = intPreferencesKey("navigation_favorites_limit")
private val REPLIES = intPreferencesKey("navigation_replies_limit")

private val TOP_BAR_TRANSPARENCY = intPreferencesKey("navigation_top_bar_transparency_percent")
private val BOTTOM_BAR_TRANSPARENCY = intPreferencesKey("navigation_bottom_bar_transparency_percent")
