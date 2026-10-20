package com.solthariel.pawchan.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

/** Pending URLs survive recreation and stay queued while application lock/recovery is shown. */
internal class IncomingLinksViewModel(
    private val saved: SavedStateHandle,
) : ViewModel() {
    val pending = saved.getStateFlow<String?>(PENDING_LINK, null)

    fun accept(
        action: String?,
        url: String?,
    ) {
        if (action == "android.intent.action.VIEW" && url != null && incomingForumLink(url) != null) {
            saved[PENDING_LINK] = url
        }
    }

    fun consume(url: String) {
        if (pending.value == url) saved[PENDING_LINK] = null
    }
}

internal val LocalIncomingLinks = staticCompositionLocalOf<IncomingLinksViewModel?> { null }
private const val PENDING_LINK = "pending-forum-link"
