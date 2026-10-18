package com.solthariel.pawchan

import com.solthariel.pawchan.thread.ThreadContentFlags
import com.solthariel.pawchan.thread.ThreadContentPresentation
import com.solthariel.pawchan.thread.ThreadContentStatistics
import com.solthariel.pawchan.thread.ThreadError
import com.solthariel.pawchan.thread.ThreadUiState
import com.solthariel.pawchan.thread.keepContentOnRefreshFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ThreadRefreshFailureTest {
    @Test
    fun threadRefreshRetainsPostsFavoriteAndChallengeLink() {
        val previous =
            ThreadUiState.Content(
                42,
                "Тред",
                emptyList(),
                ThreadContentFlags(false),
                ThreadContentPresentation(true, true),
                ThreadContentStatistics(),
            )
        val next =
            ThreadUiState
                .Error(ThreadError.PROTECTION, "https://2ch.su/b/res/42.html")
                .keepContentOnRefreshFailure(previous) as ThreadUiState.Content
        assertSame(previous.posts, next.posts)
        assertEquals(true, next.isFavorite)
        assertEquals(false, next.isRefreshing)
        assertEquals("https://2ch.su/b/res/42.html", next.presentation.refreshFailure?.challengeUrl)
    }
}
