package com.solthariel.pawchan.composer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import com.solthariel.pawchan.core.ui.TransientNotice
import kotlinx.coroutines.delay

/** Fast local work stays silent; changing activity cancels its pending notice. */
@Composable
internal fun ComposerActivityNotice(
    activity: ComposerDraftActivity,
    error: String?,
) {
    val message by key(activity) {
        produceState<String?>(null, activity) { value = delayedComposerActivityMessage(activity) }
    }
    TransientNotice(error ?: message)
}

internal suspend fun delayedComposerActivityMessage(activity: ComposerDraftActivity): String? {
    val message =
        when (activity) {
            ComposerDraftActivity.RESTORING -> "Восстановление черновика…"
            ComposerDraftActivity.LOADING_FILES -> "Чтение вложений…"
            else -> null
        }
    if (message != null) delay(COMPOSER_PROGRESS_DELAY_MS)
    return message
}

internal const val COMPOSER_PROGRESS_DELAY_MS = 500L
