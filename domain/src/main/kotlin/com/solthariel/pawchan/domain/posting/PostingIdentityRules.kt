package com.solthariel.pawchan.domain.posting

/** Board identity controls keep the service-provided icon order. */
data class PostingIdentityRules(
    val namesEnabled: Boolean = true,
    val icons: List<PostingIcon> = emptyList(),
)
