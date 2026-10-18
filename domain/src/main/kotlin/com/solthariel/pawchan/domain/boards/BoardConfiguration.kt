package com.solthariel.pawchan.domain.boards

/** null means the server did not provide a flag, not that it is disabled. */
data class BoardConfiguration(
    val subjectsEnabled: Boolean? = null,
    val namesEnabled: Boolean? = null,
)
