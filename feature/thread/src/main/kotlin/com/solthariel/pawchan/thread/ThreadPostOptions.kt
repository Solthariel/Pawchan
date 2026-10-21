package com.solthariel.pawchan.thread

internal data class ThreadPostOptions(
    val appearance: ThreadAppearance = ThreadAppearance(),
    val selecting: Boolean = false,
    val selected: Boolean = false,
    val changes: ThreadPostChanges = ThreadPostChanges(),
    val replyLabel: String? = null,
    val interactions: ThreadPostInteractions = ThreadPostInteractions(),
)

internal data class ThreadPostInteractions(
    val onReplies: () -> Unit = {},
    val onSelect: () -> Unit = {},
    val onMenu: () -> Unit = {},
    val onQuote: (String) -> Unit = {},
)
