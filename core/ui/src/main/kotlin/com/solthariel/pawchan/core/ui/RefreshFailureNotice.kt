package com.solthariel.pawchan.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun RefreshFailureNotice(
    failure: RefreshFailure?,
    onChallenge: (String) -> Unit,
) {
    val message =
        when {
            failure == null -> null
            failure.rateLimited -> stringResource(R.string.core_ui_error_rate_limited)
            else -> stringResource(R.string.core_ui_error_refresh_kept)
        }
    val action =
        failure?.challengeUrl?.let { url ->
            NoticeAction(stringResource(R.string.core_ui_challenge_open)) { onChallenge(url) }
        }
    TransientNotice(message, eventKey = failure, action = action)
}
