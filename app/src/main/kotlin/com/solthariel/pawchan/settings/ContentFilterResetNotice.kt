package com.solthariel.pawchan.settings

import androidx.compose.runtime.Composable
import com.solthariel.pawchan.core.ui.TransientNotice

@Composable
internal fun ContentFilterResetNotice(onDismiss: () -> Unit) {
    TransientNotice("Сохранённые правила автоскрытия сброшены. Создайте их заново.", onDismiss = onDismiss)
}
