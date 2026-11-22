package com.solthariel.pawchan

import com.solthariel.pawchan.thread.ThreadFreshPosts
import com.solthariel.pawchan.thread.ThreadPostChanges
import com.solthariel.pawchan.thread.ThreadRefreshNotice
import org.junit.Assert.assertEquals
import org.junit.Test

class ThreadRefreshNoticeTest {
    @Test fun dashchanMessagesUseCommasAndIncludeZeroNew() {
        assertEquals("1 новое сообщение", ThreadRefreshNotice(1, 0, 0, 7).message)
        assertEquals("1 новое, 1 удалён", ThreadRefreshNotice(1, 0, 1, 7).message)
        assertEquals("0 новых, 1 удалён", ThreadRefreshNotice(0, 0, 1, null).message)
        assertEquals("2 новых, 1 ответ, 3 удалено", ThreadRefreshNotice(2, 1, 3, 7).message)
    }

    @Test fun ownNewPostAndCyclicDeletionAreBothReported() {
        val fresh = ThreadFreshPosts()
        fresh.update(listOf(toolsPost(1), toolsPost(2)))
        fresh.update(
            listOf(
                toolsPost(1),
                toolsPost(2, changes = ThreadPostChanges(deleted = true)),
                toolsPost(3, changes = ThreadPostChanges(own = true)),
            ),
        )
        assertEquals(ThreadRefreshNotice(1, 0, 1, 3L), fresh.notice.value)
        fresh.seen(3)
        assertEquals("1 новое, 1 удалён", fresh.notice.value?.message)
    }

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
