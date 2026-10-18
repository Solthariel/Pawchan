package com.solthariel.pawchan.workspace

import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.workspace.storePost

internal fun WorkspaceController.setOwn(
    address: ThreadAddress,
    id: Long,
    own: Boolean,
) {
    val snapshot = latestSnapshot?.takeIf { it.address == address } ?: return
    val post = snapshot.posts.firstOrNull { it.address.post.value == id } ?: return
    change { state ->
        if (own) {
            state.storePost(post, snapshot.subject, own = true)
        } else {
            state.copy(
                posts =
                    state.posts.map {
                        if (it.post.address == post.address) it.copy(own = false) else it
                    },
            )
        }
    }
}
