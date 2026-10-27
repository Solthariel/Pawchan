package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.filter.ContentFilterConditionSpec
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.navigation.defaultBoardAddress
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentFilterValidationStateTest {
    private val base = newContentFilterRule(defaultBoardAddress(), "кот")

    @Test fun typingValidTextDoesNotDisableSaveWhilePreviewIsPending() =
        runTest {
            val state = ContentFilterValidationState(StandardTestDispatcher(testScheduler))
            val draft = ContentFilterDraft(base)
            listOf("к", "ко", "кот", "котик").forEach { text ->
                draft.text = text
                assertTrue(state.canSave(draft))
                state.check(draft.rule(base))
                assertTrue(state.canSave(draft))
            }
        }

    @Test fun blankConditionAndMalformedBoardCannotEnableSave() {
        val state = ContentFilterValidationState()
        val draft = ContentFilterDraft(base)
        draft.text = " \n "
        assertFalse(state.canSave(draft))
        draft.text = "кот"
        draft.boards = "!invalid!"
        assertFalse(state.canSave(draft))
    }

    @Test fun saveValidatesCurrentRuleEvenWithoutPreview() =
        runTest {
            val state = ContentFilterValidationState(StandardTestDispatcher(testScheduler))
            val draft = ContentFilterDraft(base)
            draft.text = "новый текст"
            var saved: ContentFilterRule? = null
            val current = draft.rule(base)
            state.save(current, draft.inputError(), { draft.rule(base) }) { saved = it }
            assertEquals(current, saved)
            assertFalse(state.saving)
        }

    @Test fun malformedRegexCannotBeSaved() =
        runTest {
            val state = ContentFilterValidationState(StandardTestDispatcher(testScheduler))
            val invalid = base.copy(condition = ContentFilterConditionSpec(ContentFilterMode.REGEX, listOf("[")))
            var saved = false
            state.save(invalid, null, { invalid }) { saved = true }
            assertFalse(saved)
            assertNotNull(state.errorFor(invalid))
            assertNull(state.errorFor(base))
        }

    @Test fun emptyConditionCannotBypassValidationThroughDirectSave() =
        runTest {
            val state = ContentFilterValidationState(StandardTestDispatcher(testScheduler))
            val empty = base.copy(condition = ContentFilterConditionSpec(ContentFilterMode.PHRASE, listOf("")))
            var saved = false
            state.save(empty, null, { empty }) { saved = true }
            assertFalse(saved)
            assertNotNull(state.errorFor(empty))
        }

    @Test fun editingDuringValidationDoesNotSaveAnOutdatedSnapshot() =
        runTest {
            val state = ContentFilterValidationState(StandardTestDispatcher(testScheduler))
            val draft = ContentFilterDraft(base)
            var saved = false
            val job =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    state.save(draft.rule(base), null, { draft.rule(base) }) { saved = true }
                }
            assertTrue(state.saving)
            draft.text = "другое правило"
            job.join()
            assertFalse(saved)
            assertFalse(state.saving)
        }

    @Test fun concurrentSaveRequestsProduceOneCallback() =
        runTest {
            val state = ContentFilterValidationState(StandardTestDispatcher(testScheduler))
            var saves = 0
            val job =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    state.save(base, null, { base }) { saves++ }
                }
            assertFalse(state.canSave(ContentFilterDraft(base)))
            state.save(base, null, { base }) { saves++ }
            job.join()
            assertEquals(1, saves)
        }

    @Test fun cancellingValidationDoesNotSaveAndRestoresButtonState() =
        runTest {
            val state = ContentFilterValidationState(StandardTestDispatcher(testScheduler))
            var saved = false
            val job =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    state.save(base, null, { base }) { saved = true }
                }
            job.cancel()
            job.join()
            assertFalse(saved)
            assertFalse(state.saving)
        }
}
