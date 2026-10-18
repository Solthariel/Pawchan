package com.solthariel.pawchan.thread

/** Prepared visibility plus the optional auto-hide explanation; manual hiding survives temporary reveal. */
internal data class ThreadPostVisibility(
    val hidden: Boolean = false,
    val contentFilterLabel: String? = null,
    val contentFilterId: String? = null,
)
