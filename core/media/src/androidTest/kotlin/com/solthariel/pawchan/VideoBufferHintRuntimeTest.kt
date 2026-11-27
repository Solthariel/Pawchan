package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.media.video.VIDEO_BUFFER_HINT_MS
import com.solthariel.pawchan.media.video.VideoAttempt
import com.solthariel.pawchan.media.video.VideoFailure
import com.solthariel.pawchan.media.video.VideoPlaybackStatus
import com.solthariel.pawchan.media.video.VideoStage
import com.solthariel.pawchan.media.video.VideoStateOverlay
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class VideoBufferHintRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun fastOpenIsSilentAndSlowWaitIsCancelledByReadyOrError() {
        val loading = VideoPlaybackStatus(stage = VideoStage.BUFFERING)
        val ready = loading.copy(stage = VideoStage.READY)
        val state = mutableStateOf(loading)
        var retries = 0
        compose.mainClock.autoAdvance = false
        compose.setContent { MaterialTheme { VideoStateOverlay(state.value) { retries++ } } }
        val hint = compose.onNodeWithText("Загрузка…")
        compose.mainClock.advanceTimeBy(VIDEO_BUFFER_HINT_MS / 2)
        hint.assertDoesNotExist()
        compose.runOnIdle { state.value = ready }
        compose.mainClock.advanceTimeBy(VIDEO_BUFFER_HINT_MS * 2)
        hint.assertDoesNotExist()
        compose.runOnIdle { state.value = loading }
        compose.mainClock.advanceTimeBy(VIDEO_BUFFER_HINT_MS * 2)
        hint.assertIsDisplayed()
        compose.runOnIdle { state.value = loading.copy(stage = VideoStage.ERROR, attempt = VideoAttempt(failure = VideoFailure.SOURCE)) }
        compose.mainClock.advanceTimeByFrame()
        hint.assertDoesNotExist()
        compose.onNodeWithText("Не удалось загрузить или прочитать видео.").assertIsDisplayed()
        compose.onNodeWithText("Повторить").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }
}
