package com.solthariel.pawchan.backup

import com.solthariel.pawchan.domain.workspace.WorkspaceState

internal fun replaceWorkspace(
    current: WorkspaceState,
    backup: ClientBackup,
): WorkspaceState {
    val own = BackupSection.OWN_POSTS in backup.sections
    val hidden = BackupSection.HIDDEN in backup.sections
    val incoming = backup.bookmarks
    val incomingAddresses = incoming.posts.map { it.post.address }.toSet()
    return current.copy(
        posts =
            if (own) {
                current.posts.filterNot { it.own || it.post.address in incomingAddresses } + incoming.posts
            } else {
                current.posts
            },
        replies = if (own) incoming.replies else current.replies,
        marks =
            current.marks.copy(
                hidden = if (hidden) incoming.marks.hidden else current.marks.hidden,
                dismissedReplies = if (own) emptySet() else current.marks.dismissedReplies,
            ),
    )
}
