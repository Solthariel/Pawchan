package com.solthariel.pawchan.arhivach

internal fun archiveThreadLink(id: Long?): String? = id?.takeIf { it > 0 }?.let { "https://arhivach.vc/thread/$it/" }

internal fun archivePostLink(
    threadId: Long?,
    postId: Long,
): String? = archiveThreadLink(threadId)?.takeIf { postId > 0 }?.let { "$it#$postId" }
