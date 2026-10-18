package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.ThreadAddress

internal fun publicThreadLink(
    address: ThreadAddress,
    postId: Long? = null,
): String {
    val thread = "https://2ch.hk/${address.board.board.value}/res/${address.thread.value}.html"
    return if (postId == null) thread else "$thread#$postId"
}
