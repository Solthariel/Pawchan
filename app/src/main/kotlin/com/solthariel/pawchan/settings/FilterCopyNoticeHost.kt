package com.solthariel.pawchan.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
internal fun FilterCopyNoticeHost(content: @Composable () -> Unit) {
    var request by remember { mutableIntStateOf(0) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(request) {
        if (request > 0) {
            visible = true
            delay(COPY_NOTICE_MILLIS)
            visible = false
        }
    }
    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalFilterCopyNotice provides { request++ }, content = content)
        AnimatedVisibility(
            visible,
            Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .imePadding()
                .padding(16.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shadowElevation = 6.dp,
                modifier = Modifier.testTag("filter-copy-notice"),
            ) {
                Text(
                    "Скопировано. Вставьте в чат с нейросетью",
                    Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private const val COPY_NOTICE_MILLIS = 3_000L
