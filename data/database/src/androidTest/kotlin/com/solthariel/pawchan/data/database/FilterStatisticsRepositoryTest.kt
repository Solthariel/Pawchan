package com.solthariel.pawchan.data.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.solthariel.pawchan.domain.filter.ContentFilterHit
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FilterStatisticsRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val board = BoardAddress(checkNotNull(SiteId.parse("dvach")), checkNotNull(BoardId.parse("b")))
    private val thread = ThreadAddress(board, checkNotNull(ThreadId.parse(42)))
    private val hit = ContentFilterHit("rule", thread, 42L, true)

    @Test
    fun concurrentRefreshesCannotCountTheSameMessageTwice() =
        runBlocking {
            val repository = SqliteFilterStatisticsRepository(context)
            try {
                assertTrue(repository.reset() is DomainOutcome.Success)
                val jobs = List(10) { launch { repository.record(listOf(hit, hit)) } }
                jobs.forEach { it.join() }
                val statistics = repository.statistics.first()
                assertEquals(1L, statistics.total)
                assertEquals(1L, statistics.rules["rule"])
                assertEquals(1L, statistics.originalPosts)
                assertEquals(0L, statistics.replies)
            } finally {
                repository.reset()
                repository.close()
            }
        }

    @Test
    fun siteBoardThreadRuleAndPostIdentitiesRemainIndependent() =
        runBlocking {
            val repository = SqliteFilterStatisticsRepository(context)
            try {
                repository.reset()
                val otherBoard = board.copy(board = checkNotNull(BoardId.parse("vg")))
                val otherSite = board.copy(site = checkNotNull(SiteId.parse("another")))
                repository.record(
                    listOf(
                        hit,
                        hit.copy(ruleId = "another-rule"),
                        hit.copy(thread = thread.copy(board = otherBoard)),
                        hit.copy(thread = thread.copy(board = otherSite)),
                        hit.copy(postId = 43L, originalPost = false),
                    ),
                )
                val statistics = repository.statistics.first()
                assertEquals(5L, statistics.total)
                assertEquals(3, statistics.boards.size)
                assertEquals(1L, statistics.replies)
                assertEquals(4L, statistics.rules["rule"])
            } finally {
                repository.reset()
                repository.close()
            }
        }

    @Test
    fun reopeningPreservesCountersAndResetOnlyClearsStatistics() =
        runBlocking {
            val first = SqliteFilterStatisticsRepository(context)
            first.reset()
            first.record(listOf(hit))
            first.close()
            val reopened = SqliteFilterStatisticsRepository(context)
            try {
                assertEquals(1L, reopened.statistics.first().total)
                assertTrue(reopened.reset() is DomainOutcome.Success)
                assertEquals(0L, reopened.statistics.first().total)
                assertTrue(
                    reopened
                        .statistics
                        .first()
                        .rules
                        .isEmpty(),
                )
            } finally {
                reopened.close()
            }
        }
}
