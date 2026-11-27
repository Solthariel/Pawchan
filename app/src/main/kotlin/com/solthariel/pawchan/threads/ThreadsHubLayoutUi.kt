package com.solthariel.pawchan.threads

import android.content.res.Resources
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.domain.settings.NavigationLayout
import com.solthariel.pawchan.domain.settings.NavigationPreferences
import com.solthariel.pawchan.domain.settings.ThreadsHubItem
import com.solthariel.pawchan.R.string as L

/** Разделы, которые стоят на экране «Треды», в выбранном порядке. */
internal fun threadsScreenItems(layout: NavigationLayout): List<ThreadsHubItem> =
    layout.normalized().let { normalized -> normalized.threads.filterNot { it in normalized.threadsInMenu } }

/** Ссылки, убранные в ⋮, в том же порядке. */
internal fun threadsMenuItems(layout: NavigationLayout): List<ThreadsHubItem> =
    layout.normalized().let { normalized -> normalized.threads.filter { it in normalized.threadsInMenu } }

internal fun ThreadsHubItem.section(): ThreadsSection? =
    when (this) {
        ThreadsHubItem.PAGES -> ThreadsSection.PAGES
        ThreadsHubItem.FAVORITES -> ThreadsSection.FAVORITES
        ThreadsHubItem.REPLIES -> ThreadsSection.REPLIES
        else -> null
    }

internal fun Resources.threadsHubLabel(item: ThreadsHubItem): String =
    getString(
        when (item) {
            ThreadsHubItem.PAGES -> L.ui_pages
            ThreadsHubItem.FAVORITES -> L.ui_favorite_threads
            ThreadsHubItem.REPLIES -> L.ui_replies
            ThreadsHubItem.HISTORY -> L.ui_history
            ThreadsHubItem.COPIES -> L.ui_saved_copies
            ThreadsHubItem.DOWNLOADS -> L.ui_downloads
        },
    )

internal fun ThreadsHubItem.linkIcon(): UiIconKind =
    when (this) {
        ThreadsHubItem.HISTORY -> UiIconKind.DOCUMENT
        else -> UiIconKind.SAVE
    }

internal fun ThreadsLinkActions.open(item: ThreadsHubItem) {
    when (item) {
        ThreadsHubItem.HISTORY -> history()
        ThreadsHubItem.COPIES -> copies()
        ThreadsHubItem.DOWNLOADS -> downloads()
        else -> Unit
    }
}

/** «Сбросить» в «Отображении списка»: порядок, ⋮ и число строк, остальная навигация не трогается. */
internal fun NavigationPreferences.withThreadsDefaults(): NavigationPreferences {
    val defaults = NavigationPreferences()
    return copy(
        pages = defaults.pages,
        favorites = defaults.favorites,
        replies = defaults.replies,
        layout = layout.copy(threads = ThreadsHubItem.entries, threadsInMenu = emptySet()),
    )
}
