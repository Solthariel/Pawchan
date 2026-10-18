package com.solthariel.pawchan.thread

import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.ThreadSnapshot

/** App owns durable workspace state; a thread session delivers addressed events only. */
interface ThreadWorkspaceEvents {
    fun observe(snapshot: ThreadSnapshot)

    fun storeOwn(post: BoardPost)
}
