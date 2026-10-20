package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.R

@Composable
fun LoadErrorPane(
    challengeUrl: String? = null,
    onChallenge: (String) -> Unit = {},
    rateLimited: Boolean = false,
    message: String? = null,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        fullScreenErrorItem(challengeUrl, onChallenge, rateLimited, message)
    }
}

private fun LazyListScope.fullScreenErrorItem(
    challengeUrl: String?,
    onChallenge: (String) -> Unit,
    rateLimited: Boolean,
    customMessage: String?,
) {
    val message = if (rateLimited) R.string.core_ui_error_rate_limited else R.string.core_ui_error_loading_data
    item {
        Box(
            modifier = Modifier.fillParentMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "⚠",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    text = customMessage ?: stringResource(message),
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
                challengeUrl?.let { url ->
                    Button(onClick = { onChallenge(url) }) {
                        Text(stringResource(R.string.core_ui_challenge_open))
                    }
                }
            }
        }
    }
}
