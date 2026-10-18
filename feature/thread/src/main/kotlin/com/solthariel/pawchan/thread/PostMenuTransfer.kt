package com.solthariel.pawchan.thread

import com.solthariel.pawchan.domain.model.ThreadAddress

interface PostMenuTransfer {
    fun copy(text: String)

    fun postLink(
        address: ThreadAddress,
        postId: Long,
    ): String? = null
}
