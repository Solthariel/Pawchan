package com.solthariel.pawchan.workspace

import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.workspace.markRead

internal fun WorkspaceController.readReplies(item: WorkspacePostUi) =
    change { state ->
        state.markRead(
            state.replies
                .filter { reply ->
                    reply.post.address.thread == item.address &&
                        reply.post.content.document.runs.any { run ->
                            val reference = run.link as? PostLink.Reference
                            reference?.post?.value == item.id &&
                                (reference?.thread == null || reference.thread == item.address)
                        }
                }.map { it.post.address }
                .toSet(),
        )
    }
