package com.solthariel.pawchan.domain.filter

/** Only the literal builder executes. Old wire names are recognized solely to reject them explicitly. */
val ContentFilterMode.isCurrentFilterMode: Boolean get() = isBuilderMode
