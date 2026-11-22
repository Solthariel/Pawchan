package com.solthariel.pawchan

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.solthariel.pawchan.composer.ComposerCaptchaState
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.ComposerFailureState
import com.solthariel.pawchan.composer.ComposerProgressState
import com.solthariel.pawchan.composer.ComposerScreenState
import com.solthariel.pawchan.composer.ComposerScreenStatus
import com.solthariel.pawchan.composer.ComposerSendFlow
import com.solthariel.pawchan.composer.ReplyError
import com.solthariel.pawchan.composer.rememberComposerSendFlow
import com.solthariel.pawchan.domain.posting.PostingRules
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ComposerModalSendRuntimeTest {
    @get:Rule val compose = createComposeRule()

    private fun initial() =
        ComposerScreenState(
            ComposerDraftState(comment = "text"),
            PostingRules(),
            ComposerCaptchaState(),
            ComposerScreenStatus(ComposerProgressState(), ComposerFailureState(), false, canRequestSubmit = true),
        )

    @Test fun silentPreloadCannotSendAndAnArmedSolutionSendsOnlyOnce() {
        val state = mutableStateOf(initial())
        lateinit var flow: ComposerSendFlow
        var sends = 0
        compose.setContent { flow = rememberComposerSendFlow(state.value) { sends++ } }
        compose.runOnIdle {
            assertEquals(0, sends)
            flow.open(state.value, 0)
        }
        compose.runOnIdle {
            state.value =
                state.value.copy(
                    captcha = ComposerCaptchaState(satisfied = true),
                    status = state.value.status.copy(canSubmit = true),
                )
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(1, sends)
            state.value = state.value.copy(draft = state.value.draft.copy(email = "x"))
        }
        compose.runOnIdle { assertEquals(1, sends) }
    }

    @Test fun dismissCancelsTheIntentBeforeALateServerSolution() {
        val state = mutableStateOf(initial())
        lateinit var flow: ComposerSendFlow
        var sends = 0
        compose.setContent { flow = rememberComposerSendFlow(state.value) { sends++ } }
        compose.runOnIdle {
            flow.open(state.value, 0)
            flow.dismiss()
        }
        compose.runOnIdle {
            state.value =
                state.value.copy(
                    captcha = ComposerCaptchaState(satisfied = true),
                    status = state.value.status.copy(canSubmit = true),
                )
        }
        compose.runOnIdle { assertEquals(0, sends) }
    }

    @Test fun networkFailureDoesNotAutomaticallyRetry() {
        val state = mutableStateOf(initial())
        lateinit var flow: ComposerSendFlow
        var sends = 0
        compose.setContent { flow = rememberComposerSendFlow(state.value) { sends++ } }
        compose.runOnIdle { flow.open(state.value, 0) }
        compose.runOnIdle {
            state.value = state.value.copy(status = state.value.status.copy(failure = ComposerFailureState(ReplyError.NETWORK)))
        }
        compose.waitForIdle()
        compose.runOnIdle {
            state.value =
                state.value.copy(
                    captcha = ComposerCaptchaState(satisfied = true),
                    status = state.value.status.copy(canSubmit = true),
                )
        }
        compose.runOnIdle { assertEquals(0, sends) }
    }
}
