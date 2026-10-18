package com.solthariel.pawchan.infrastructure.thread

import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.thread.ThreadWorkspaceEvents
import com.solthariel.pawchan.workspace.WorkspaceController

/** Borrowed app controller; no new scope, persistence or resource ownership. */
internal class AppThreadWorkspaceEvents(
    private val workspace: WorkspaceController,
) : ThreadWorkspaceEvents {
    override fun observe(snapshot: ThreadSnapshot) {
        workspace.observe(snapshot)
    }

    override fun storeOwn(post: BoardPost) {
        workspace.storeOwn(post)
    }
}
