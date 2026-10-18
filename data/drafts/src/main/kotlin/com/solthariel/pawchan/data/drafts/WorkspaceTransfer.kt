package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.workspace.ThreadMarks
import com.solthariel.pawchan.domain.workspace.WorkspaceState

object WorkspaceTransfer {
    fun select(
        state: WorkspaceState,
        hidden: Boolean,
        own: Boolean,
    ): WorkspaceState =
        WorkspaceState(
            posts =
                state.posts.filter { own && it.own }.map {
                    it.copy(own = own && it.own, tracking = own && it.tracking)
                },
            replies = if (own) state.replies else emptyList(),
            marks = ThreadMarks(hidden = if (hidden) state.marks.hidden else emptySet()),
        )

    fun encode(state: WorkspaceState): String = encodeWorkspace(select(state, true, true))

    fun decode(value: String): WorkspaceState = select(decodeWorkspace(value), true, true)

    fun merge(
        current: WorkspaceState,
        incoming: WorkspaceState,
    ): WorkspaceState {
        val known = current.posts.map { it.post.address }.toSet()
        val additions = incoming.posts.associateBy { it.post.address }
        return current.copy(
            posts =
                current.posts.map { old ->
                    additions[old.post.address]?.let { next ->
                        old.copy(
                            own = old.own || next.own,
                            tracking = old.tracking || next.tracking,
                        )
                    } ?: old
                } + incoming.posts.filterNot { it.post.address in known },
            replies = (current.replies + incoming.replies).distinctBy { it.post.address },
            marks = current.marks.copy(hidden = current.marks.hidden + incoming.marks.hidden),
        )
    }
}
