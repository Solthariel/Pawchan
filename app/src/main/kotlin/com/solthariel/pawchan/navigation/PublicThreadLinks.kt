package com.solthariel.pawchan.navigation

import com.solthariel.pawchan.domain.model.ThreadAddress

internal fun publicThreadLink(
    address: ThreadAddress,
    postId: Long? = null,
    mirror: String = "2ch.su",
): String? =
    com.solthariel.pawchan.infrastructure.sites
        .appForumWebLinks(mirror)
        .firstOrNull { it.site == address.board.site }
        ?.threadUrl(address, postId)
