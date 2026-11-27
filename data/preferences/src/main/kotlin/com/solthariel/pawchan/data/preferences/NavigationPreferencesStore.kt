package com.solthariel.pawchan.data.preferences

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.solthariel.pawchan.domain.settings.BottomBarButton
import com.solthariel.pawchan.domain.settings.NavigationLayout
import com.solthariel.pawchan.domain.settings.NavigationPreferences
import com.solthariel.pawchan.domain.settings.ThreadsHubItem
import com.solthariel.pawchan.domain.settings.bottomBarOrderValue
import com.solthariel.pawchan.domain.settings.parseBottomBarOrder
import com.solthariel.pawchan.domain.settings.parseThreadsHubItems
import com.solthariel.pawchan.domain.settings.threadsHubValue

internal fun Preferences.readNavigation(): NavigationPreferences =
    NavigationPreferences(
        this[PAGES] ?: NavigationPreferences().pages,
        this[FAVORITES] ?: NavigationPreferences().favorites,
        this[REPLIES] ?: NavigationPreferences().replies,
        this[BOTTOM_BAR_TRANSPARENCY] ?: NavigationPreferences.DEFAULT_BOTTOM_BAR_TRANSPARENCY,
        this[TOP_BAR_TRANSPARENCY] ?: NavigationPreferences.DEFAULT_TOP_BAR_TRANSPARENCY,
        layout =
            NavigationLayout(
                this[BOTTOM_BAR_ORDER]?.let(::parseBottomBarOrder) ?: BottomBarButton.entries,
                this[THREADS_ORDER]?.let(::parseThreadsHubItems) ?: ThreadsHubItem.entries,
                this[THREADS_IN_MENU]?.let(::parseThreadsHubItems)?.toSet().orEmpty(),
            ),
        autoOpenKeyboard = this[AUTO_OPEN_KEYBOARD] ?: true,
    ).normalized()

internal fun MutablePreferences.writeNavigation(value: NavigationPreferences) {
    val normalized = value.normalized()
    this[PAGES] = normalized.pages
    this[FAVORITES] = normalized.favorites
    this[REPLIES] = normalized.replies
    this[BOTTOM_BAR_TRANSPARENCY] = normalized.bottomBarTransparencyPercent
    this[TOP_BAR_TRANSPARENCY] = normalized.topBarTransparencyPercent
    this[BOTTOM_BAR_ORDER] = normalized.bottomBarOrder.bottomBarOrderValue()
    this[THREADS_ORDER] = normalized.layout.threads.threadsHubValue()
    this[THREADS_IN_MENU] = normalized.layout.threadsInMenu.threadsHubValue()
    this[AUTO_OPEN_KEYBOARD] = normalized.autoOpenKeyboard
}

private val PAGES = intPreferencesKey("navigation_pages_limit")
private val FAVORITES = intPreferencesKey("navigation_favorites_limit")
private val REPLIES = intPreferencesKey("navigation_replies_limit")

private val TOP_BAR_TRANSPARENCY = intPreferencesKey("navigation_top_bar_transparency_percent")
private val BOTTOM_BAR_TRANSPARENCY = intPreferencesKey("navigation_bottom_bar_transparency_percent")
private val BOTTOM_BAR_ORDER = stringPreferencesKey("navigation_bottom_bar_order")
private val AUTO_OPEN_KEYBOARD = booleanPreferencesKey("navigation_auto_open_keyboard")
private val THREADS_ORDER = stringPreferencesKey("navigation_threads_order")
private val THREADS_IN_MENU = stringPreferencesKey("navigation_threads_in_menu")
