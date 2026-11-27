package com.solthariel.pawchan.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/** «Интерфейс → Сразу открывать клавиатуру»; числовые диалоги открывают её всегда и сюда не смотрят. */
val LocalAutoOpenKeyboard = staticCompositionLocalOf { true }

/**
 * Поле, ради которого его открыли (поиск, выбор доски, новое сообщение), само берёт фокус и клавиатуру.
 * Один раз за открытие: возврат на экран с уже открытым полем (вкладка, тред) клавиатуру не поднимает.
 * [open] = false сбрасывает открытие, [ready] откладывает фокус, пока поле недоступно.
 */
@Composable
fun FocusWhenOpened(
    focus: FocusRequester,
    open: Boolean = true,
    ready: Boolean = true,
) {
    val enabled = LocalAutoOpenKeyboard.current
    val keyboard = LocalSoftwareKeyboardController.current
    var handled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(open, ready) {
        when {
            !open -> {
                handled = false
            }

            ready && !handled -> {
                handled = true
                if (enabled) {
                    focus.requestFocus()
                    keyboard?.show()
                }
            }
        }
    }
}
