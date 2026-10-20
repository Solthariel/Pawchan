package com.solthariel.pawchan.navigation

import androidx.navigation.NavController
import com.solthariel.pawchan.infrastructure.sites.ForumWebLink
import com.solthariel.pawchan.workspace.WorkspaceController

internal fun NavController.openIncomingLink(
    link: ForumWebLink,
    workspace: WorkspaceController?,
) {
    when (link) {
        is ForumWebLink.Board -> {
            navigateBoard(link.board)
        }

        is ForumWebLink.Thread -> {
            workspace?.offlineAddress = null
            workspace?.requestedPost = link.postId
            val current = currentBackStackEntry
            val reopen =
                current?.destination?.route == AppRoute.THREAD &&
                    current.arguments?.toThreadAddress() == link.thread && link.postId != null
            // Recreate the screen's reading anchor, but retain its existing thread ViewModel/cache.
            if (reopen) popBackStack()
            navigateThread(link.thread)
        }

        is ForumWebLink.Media -> {
            Unit
        }
    }
}
