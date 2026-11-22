package com.solthariel.pawchan.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNoticeStateTest {
    @Test fun latestMessageReplacesOldWithoutAQueue() {
        val state = AppNoticeState()
        var dismissed = 0
        val first = AppNotice("first", null) { dismissed++ }
        val second = AppNotice("second", null) { dismissed++ }
        state.show(first)
        state.show(second)
        assertSame(second, state.current)
        assertEquals(0, dismissed)
        state.dismiss(first)
        assertSame(second, state.current)
        state.dismiss(second)
        assertNull(state.current)
        assertEquals(1, dismissed)
        state.dismiss(second)
        assertEquals(1, dismissed)
    }

    @Test fun repeatedIdenticalTextIsANewEvent() {
        val state = AppNoticeState()
        val first = AppNotice("Ошибка", null) {}
        val second = AppNotice("Ошибка", null) {}
        state.show(first)
        state.dismiss(first)
        state.show(second)
        assertSame(second, state.current)
    }

    @Test fun longMessagesGetLongerButBoundedReadingTime() {
        assertEquals(6_000L, noticeReadingMillis(0))
        assertEquals(6_000L, noticeReadingMillis(-1))
        assertTrue(noticeReadingMillis(800) > noticeReadingMillis(80))
        assertEquals(90_000L, noticeReadingMillis(Int.MAX_VALUE))
    }
}
