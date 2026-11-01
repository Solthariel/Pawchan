package com.solthariel.pawchan.threads

/** Pages show open-thread count, other sections show unread posts/replies. Expansion is irrelevant. */
internal fun threadsSectionCounter(
    section: ThreadsSection,
    rows: List<ThreadRowUi>?,
): ThreadCounter =
    ThreadCounter(
        if (section == ThreadsSection.PAGES) rows.orEmpty().size else rows.orEmpty().sumOf { it.counter.count },
    )
