package com.solthariel.pawchan.composer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.solthariel.pawchan.core.ui.TransientNotice
import kotlinx.coroutines.delay

@Composable
internal fun AttachmentSaveStatus(state: AttachmentSaveState) {
    val showProgress by produceState(false, state.busy) {
        value = false
        if (state.busy) {
            delay(SAVE_NOTICE_DELAY_MS)
            value = true
        }
    }
    val message = state.error ?: if (showProgress) "Сохранение изображения…" else null
    TransientNotice(message, eventKey = state.noticeVersion to state.busy)
}

private const val SAVE_NOTICE_DELAY_MS = 350L
