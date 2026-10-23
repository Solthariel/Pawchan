package com.solthariel.pawchan.navigation.chrome

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.threads.unreadReplies
import com.solthariel.pawchan.workspace.LocalWorkspace

/** Бейдж «Тредов» — только непрочитанные ответы вам. */
@Composable
internal fun rememberAppUnreadRepliesBadge(): Int {
    val state =
        LocalWorkspace.current
            ?.controller
            ?.state
            ?.collectAsStateWithLifecycle()
            ?.value ?: return 0
    return remember(state.posts, state.replies, state.marks.dismissedReplies) { unreadReplies(state) }
}
