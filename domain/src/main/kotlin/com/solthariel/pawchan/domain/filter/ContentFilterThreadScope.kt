package com.solthariel.pawchan.domain.filter

internal fun ContentFilterRule.matchesThreadId(threadId: Long?): Boolean =
    targets.threadIds.isEmpty() || (threadId != null && threadId in targets.threadIds)
