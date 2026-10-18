package com.solthariel.pawchan.domain.statistics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PostCountTest {
    @Test fun commandMustBeASeparateToken() {
        listOf("/postcount", ">>237685217\n/postcount", "text /postcount more").forEach {
            assertTrue(hasPostCountCommand(it))
        }
        listOf("https://example.org/postcount", "/postcounter", "abc/postcount").forEach {
            assertFalse(hasPostCountCommand(it))
        }
    }

    @Test fun includesEveryMetricAndAllDaysWithoutChangingTheTypedPost() {
        val text = ">>237685217\n/postcount"
        val days =
            listOf(
                UsageDay(1, mapOf(UsageMetric.POSTS_SENT to 3)),
                UsageDay(50, mapOf(UsageMetric.POSTS_SENT to 4, UsageMetric.STICKERS_HIDDEN to 8)),
            )
        val expanded = appendPostCount(text, days)
        assertTrue(expanded.startsWith(text + "\n\n"))
        assertTrue(expanded.contains("Отправлено постов: 7"))
        assertTrue(expanded.contains("Скрыто стикеров: 8"))
        assertEquals(UsageMetric.entries.size + 4, expanded.lines().size)
        UsageMetric.entries.forEach { assertTrue(expanded.contains(it.usageLabel() + ":")) }
    }

    @Test fun emptyStatisticsContainsZeros() {
        val text = appendPostCount("/postcount", emptyList())
        UsageMetric.entries.forEach { assertTrue(text.contains(it.usageLabel() + ": 0")) }
    }
}
