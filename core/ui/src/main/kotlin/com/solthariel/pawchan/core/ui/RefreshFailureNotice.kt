package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.R

@Composable
fun RefreshFailureNotice(
    failure: RefreshFailure?,
    onChallenge: (String) -> Unit,
) {
    if (failure == null) return
    val message = if (failure.rateLimited) R.string.core_ui_error_rate_limited else R.string.core_ui_error_refresh_kept
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Text(
            text = stringResource(message),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        failure.challengeUrl?.let { url ->
            TextButton(onClick = { onChallenge(url) }) { Text(stringResource(R.string.core_ui_challenge_open)) }
        }
    }
}
