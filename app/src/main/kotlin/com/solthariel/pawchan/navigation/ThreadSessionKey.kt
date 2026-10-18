package com.solthariel.pawchan.navigation

import com.solthariel.pawchan.domain.model.ThreadAddress

/** Includes the source and the session role; never uses a bare thread number. */
internal fun threadSessionKey(
    role: String,
    address: ThreadAddress,
): String = "$role:${address.site.value}/${address.board.board.value}/${address.thread.value}"
