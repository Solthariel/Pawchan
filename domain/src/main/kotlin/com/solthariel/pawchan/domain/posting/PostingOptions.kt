package com.solthariel.pawchan.domain.posting

/** Values sent to the service; thread ownership remains server-verified through cookies. */
data class PostingOptions(
    val name: String = "",
    val sage: Boolean = false,
    val threadAuthor: Boolean = false,
    val icon: Int? = null,
    val tag: String = "",
)
