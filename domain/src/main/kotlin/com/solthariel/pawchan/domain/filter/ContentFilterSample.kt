package com.solthariel.pawchan.domain.filter

import com.solthariel.pawchan.domain.model.BoardAddress

/** A snapshot of already loaded content. Testing never requests more data. */
data class ContentFilterSample(
    val board: BoardAddress,
    val subject: String?,
    val text: String,
    val comment: Boolean,
    val place: ContentFilterPlace = ContentFilterPlace.THREAD,
    val threadId: Long? = null,
)
