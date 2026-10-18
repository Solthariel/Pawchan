package com.solthariel.pawchan.thread

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.AppDialog
import com.solthariel.pawchan.core.ui.AppDialogStyle
import com.solthariel.pawchan.core.ui.AppearanceScale
import com.solthariel.pawchan.core.ui.AppearanceSlider

@Composable
internal fun ThreadAppearanceDialog(
    value: ThreadAppearance,
    onChange: (ThreadAppearance) -> Unit,
    onClose: () -> Unit,
) {
    AppDialog(onClose) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(AppDialogStyle.contentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Внешний вид", style = MaterialTheme.typography.titleLarge)
            AppearanceSlider(
                "Текст поста: ${value.textSize} sp",
                value.textSize,
                AppearanceScale(MIN_TEXT_SIZE..MAX_TEXT_SIZE, DEFAULT_THREAD_TEXT_SIZE, "sp"),
            ) { onChange(value.copy(textSize = it)) }
            AppearanceSlider(
                "Сворачивать после: ${value.maxLines} строк",
                value.maxLines,
                AppearanceScale(MIN_POST_LINES..MAX_POST_LINES, DEFAULT_THREAD_MAX_LINES, "строк"),
            ) { onChange(value.copy(maxLines = it)) }
            TextButton(onClick = { onChange(ThreadAppearance()) }) { Text("По умолчанию") }
        }
    }
}

private const val MIN_TEXT_SIZE = 6
private const val MAX_TEXT_SIZE = 18
private const val MIN_POST_LINES = 10
private const val MAX_POST_LINES = 30
