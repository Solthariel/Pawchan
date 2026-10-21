package com.solthariel.pawchan.domain.workspace

import com.solthariel.pawchan.domain.model.ThreadAddress

fun WorkspaceState.withReadingPosition(
    address: ThreadAddress,
    postId: Long,
    offset: Int,
): WorkspaceState =
    copy(
        tabs = tabs.map { if (it.address == address) it.copy(postId = postId, offset = offset) else it },
        copies =
            copies.map {
                if (it.snapshot.address == address) {
                    it.copy(position = OpenThread(address, it.snapshot.subject, postId, offset))
                } else {
                    it
                }
            },
    )
