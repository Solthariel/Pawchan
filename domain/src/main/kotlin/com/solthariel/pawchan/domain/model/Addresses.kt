package com.solthariel.pawchan.domain.model

sealed interface ContentAddress {
    val site: SiteId
}

data class BoardAddress(
    override val site: SiteId,
    val board: BoardId,
) : ContentAddress

data class ThreadAddress(
    val board: BoardAddress,
    val thread: ThreadId,
) : ContentAddress {
    override val site: SiteId
        get() = board.site
}

data class PostAddress(
    val thread: ThreadAddress,
    val post: PostId,
) : ContentAddress {
    override val site: SiteId
        get() = thread.site
}
