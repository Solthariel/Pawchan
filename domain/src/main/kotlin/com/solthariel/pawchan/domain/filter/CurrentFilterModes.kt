package com.solthariel.pawchan.domain.filter

/** Legacy enum names are retained only to read and recover stored user data, not to execute it. */
val ContentFilterMode.isCurrentFilterMode: Boolean
    get() = this == ContentFilterMode.EXACT_PHRASE || this == ContentFilterMode.REGEX
