package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.PageLoadingIndicator
import kotlinx.coroutines.delay
import com.solthariel.pawchan.feature.composer.R.string as L

@Composable
internal fun CaptchaPendingIndicator(checking: Boolean) {
    val res = LocalResources.current

    var delayed by remember { mutableStateOf(false) }
    LaunchedEffect(checking) {
        delayed = false
        if (checking) {
            delay(COMPOSER_PROGRESS_DELAY_MS)
            delayed = true
        }
    }
    Box(Modifier.width(100.dp), contentAlignment = Alignment.Center) {
        if (checking && delayed) {
            PageLoadingIndicator(
                description = res.getString(L.ui_checking_captcha),
                size = 16.dp,
                backgroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                foregroundColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
