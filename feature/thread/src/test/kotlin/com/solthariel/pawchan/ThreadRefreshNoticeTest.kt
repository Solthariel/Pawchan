package com.solthariel.pawchan

import com.solthariel.pawchan.thread.ThreadRefreshNotice
import org.junit.Assert.assertEquals
import org.junit.Test

class ThreadRefreshNoticeTest {
    @Test fun mergeAccumulatesCountsAndKeepsEarliestNewPost() {
        val shown = ThreadRefreshNotice(newPosts = 2, replies = 1, deleted = 0, firstNew = 10L)
        val next = ThreadRefreshNotice(newPosts = 3, replies = 0, deleted = 1, firstNew = 20L)

        val merged = shown.merge(next)

        assertEquals(ThreadRefreshNotice(newPosts = 5, replies = 1, deleted = 1, firstNew = 10L), merged)
    }

    @Test fun mergeTakesFirstNewPostWhenOnlyDeletionsWereShown() {
        val shown = ThreadRefreshNotice(newPosts = 0, replies = 0, deleted = 1, firstNew = null)

        assertEquals(30L, shown.merge(ThreadRefreshNotice(1, 0, 0, 30L)).firstNew)
    }
}
