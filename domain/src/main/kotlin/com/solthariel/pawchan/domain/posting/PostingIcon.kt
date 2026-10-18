package com.solthariel.pawchan.domain.posting

/** Board-specific identity badge; list order is supplied by the service. */
data class PostingIcon(
    val id: Int,
    val name: String,
    val imageUrl: String? = null,
)
