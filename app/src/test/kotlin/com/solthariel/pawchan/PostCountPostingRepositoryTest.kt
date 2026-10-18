package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.posting.NewThreadDraft
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import com.solthariel.pawchan.domain.posting.ReplyDraft
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.settings.AppSettingsRepository
import com.solthariel.pawchan.domain.statistics.UsageDay
import com.solthariel.pawchan.domain.statistics.UsageMetric
import com.solthariel.pawchan.domain.statistics.UsageStatisticsRepository
import com.solthariel.pawchan.infrastructure.PostCountPostingRepository
import com.solthariel.pawchan.navigation.boardAddress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PostCountPostingRepositoryTest {
    private val board = requireNotNull(boardAddress("dvach", "b"))
    private val address = ThreadAddress(board, requireNotNull(ThreadId.parse(42)))

    @Test fun sendAndRetryExpandExactlyOnceWithoutMutatingDraft() =
        runBlocking {
            val delegate = FakePostingRepository()
            val repository = PostCountPostingRepository(delegate, Settings(), Statistics())
            val draft = ReplyDraft(">>42\n/postcount", null, emptyList())
            repeat(2) {
                assertTrue(repository.submitReply(address, draft) is PostingOutcome.Success)
                val sent = requireNotNull(delegate.lastDraft).comment
                assertTrue(sent.startsWith(draft.comment + "\n\n"))
                assertEquals(1, sent.split("Статистика за всё время:").size - 1)
                assertTrue(sent.contains("Отправлено постов: 7"))
            }
            assertEquals(">>42\n/postcount", draft.comment)
        }

    @Test fun disabledCommandIsSentLiterally() =
        runBlocking {
            val delegate = FakePostingRepository()
            val settings = Settings()
            settings.appSettings.value =
                AppSettings(
                    behavior =
                        com.solthariel.pawchan.domain.settings
                            .AppBehavior(postCountEnabled = false),
                )
            PostCountPostingRepository(delegate, settings, Statistics())
                .submitReply(address, ReplyDraft("/postcount", null, emptyList()))
            assertEquals("/postcount", delegate.lastDraft?.comment)
            assertEquals(0, delegate.ruleLoads)
        }

    @Test fun lengthFailureDoesNotSendOrTruncateStatistics() =
        runBlocking {
            val delegate = FakePostingRepository(PostingRules(maxCommentLength = 30))
            val result =
                PostCountPostingRepository(delegate, Settings(), Statistics())
                    .submitReply(address, ReplyDraft("/postcount", null, emptyList()))
            assertTrue(result is PostingOutcome.Failure)
            assertNull(delegate.lastDraft)
        }

    @Test fun unreadableStatisticsDoesNotSilentlyPublishZeros() =
        runBlocking {
            val delegate = FakePostingRepository()
            val stats = Statistics()
            stats.available.value = false
            val result =
                PostCountPostingRepository(delegate, Settings(), stats)
                    .submitReply(address, ReplyDraft("/postcount", null, emptyList()))
            assertTrue(result is PostingOutcome.Failure)
            assertNull(delegate.lastDraft)
        }

    @Test fun noCommandDoesNotLoadRulesAndThreadCreationAlsoExpands() =
        runBlocking {
            val delegate = FakePostingRepository()
            val repository = PostCountPostingRepository(delegate, Settings(), Statistics())
            repository.submitReply(address, ReplyDraft("ordinary text", null, emptyList()))
            assertEquals(0, delegate.ruleLoads)
            val files = listOf(ReplyAttachment("x.png", "image/png", byteArrayOf(1)))
            val result = repository.createThread(board, NewThreadDraft("title", "/postcount", null, files))
            assertTrue(result is PostingOutcome.Success)
            assertEquals(1, delegate.ruleLoads)
        }

    private class Settings : AppSettingsRepository {
        override val appSettings = MutableStateFlow(AppSettings())

        override suspend fun updateAppSettings(transform: (AppSettings) -> AppSettings): DomainOutcome<Unit> {
            appSettings.value = transform(appSettings.value)
            return DomainOutcome.Success(Unit)
        }
    }

    private class Statistics : UsageStatisticsRepository {
        override val days = MutableStateFlow(listOf(UsageDay(1, mapOf(UsageMetric.POSTS_SENT to 7))))
        override val available = MutableStateFlow(true)

        override suspend fun record(
            metric: UsageMetric,
            amount: Long,
            identity: String?,
        ) = Unit

        override suspend fun reset() = Unit
    }
}
