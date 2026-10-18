package com.solthariel.pawchan.thread

import com.solthariel.pawchan.domain.model.PostAddress

internal fun postDownloadPrefix(address: PostAddress): String =
    listOf(
        address.thread.site.value,
        address.thread.board.board.value,
        address.thread.thread.value
            .toString(),
        address.post.value.toString(),
    ).joinToString("_")
