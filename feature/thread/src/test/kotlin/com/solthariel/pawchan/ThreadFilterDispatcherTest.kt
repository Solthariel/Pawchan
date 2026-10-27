package com.solthariel.pawchan

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.solthariel.pawchan.domain.filter.ContentFilterConditionSpec
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.filter.contentFilterValidationError
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.settings.AppSettingsRepository
import com.solthariel.pawchan.thread.ThreadPersistence
import com.solthariel.pawchan.thread.ThreadUiState
import com.solthariel.pawchan.thread.ThreadViewModel
import com.solthariel.pawchan.thread.ThreadViewModelEnvironment
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class ThreadFilterDispatcherTest {
    private val address = defaultThreadAddress(42)
    private val rules =
        listOf(
            ContentFilterRule(
                "filter",
                address.site.value,
                condition = ContentFilterConditionSpec(values = listOf("скрыть")),
            ),
        )

    @Test
    fun ruleChangesUseInjectedDispatcherAndPublishBeforeSchedulerBecomesIdle() =
        runTest {
            assertNull(contentFilterValidationError(rules.single()))
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val owner = ViewModelStore()
            try {
                val settings = DispatcherTestSettings()
                val dispatcher = RecordingFilterDispatcher(StandardTestDispatcher(testScheduler))
                val model = model(owner, settings, dispatcher)
                advanceUntilIdle()
                val beforeRuleUpdate = dispatcher.dispatches
                settings.values.value = settings.values.value.withContentFilters(rules)
                advanceUntilIdle()
                assertTrue(dispatcher.dispatches > beforeRuleUpdate)
                val filtered = model.state.value as ThreadUiState.Content
                val op = filtered.posts.first()
                val reply = filtered.posts.last()
                assertEquals(rules.single().label, filtered.presentation.contentFilterNotice)
                assertFalse(op.metadata.hidden)
                assertTrue(reply.metadata.hidden)
                model.revealContentFilter(reply.id)
                advanceUntilIdle()
                val revealed = model.state.value as ThreadUiState.Content
                val revealedReply = revealed.posts.last()
                assertFalse(revealedReply.metadata.hidden)
            } finally {
                owner.clear()
                runCurrent()
                Dispatchers.resetMain()
            }
        }

    private fun model(
        owner: ViewModelStore,
        settings: AppSettingsRepository,
        dispatcher: CoroutineDispatcher,
    ): ThreadViewModel {
        val snapshot =
            ThreadSnapshot(address, "thread", ThreadFlags(false, false, false), listOf(post(42), post(43)))
        val factory =
            viewModelFactory {
                initializer {
                    ThreadViewModel(
                        address,
                        FakeThreadRepository(DomainOutcome.Success(snapshot)),
                        ThreadPersistence(FakeThreadLibrary(), FakeWatchRepository(), appSettings = settings),
                        ThreadViewModelEnvironment(uiDispatcher = dispatcher),
                    )
                }
            }
        return ViewModelProvider(owner, factory)[ThreadViewModel::class.java]
    }

    private fun post(id: Long): BoardPost =
        BoardPost(
            PostAddress(address, requireNotNull(PostId.parse(id))),
            PostContent(PostDocument.plain("скрыть"), PostMetadata(null, null, false)),
            emptyList(),
        )
}

private class RecordingFilterDispatcher(
    private val delegate: CoroutineDispatcher,
) : CoroutineDispatcher() {
    var dispatches: Int = 0
        private set

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        dispatches++
        delegate.dispatch(context, block)
    }
}

private class DispatcherTestSettings : AppSettingsRepository {
    val values = MutableStateFlow(AppSettings())
    override val appSettings = values

    override suspend fun updateAppSettings(transform: (AppSettings) -> AppSettings): DomainOutcome<Unit> {
        values.value = transform(values.value)
        return DomainOutcome.Success(Unit)
    }
}
