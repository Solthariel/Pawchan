package com.solthariel.pawchan.catalog

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController

/**
 * Поиск в шапке каталога. Клавиатура появляется только по действию пользователя (лупа, крестик).
 * После возврата из треда пустой поиск закрыт, а непустой остаётся с текстом, но без фокуса и клавиатуры.
 */
internal class CatalogSearchState(
    private val openedState: MutableState<Boolean>,
    private val focusManager: FocusManager,
    private val keyboard: SoftwareKeyboardController?,
) {
    val focusRequester = FocusRequester()
    internal var query by mutableStateOf("")
    internal var onQueryChange: (String) -> Unit = {}

    /** Пользователь работал с поиском на этом экране; после возврата из треда — снова false. */
    private var touched by mutableStateOf(false)
    var focusRequested by mutableStateOf(false)
        private set

    val visible: Boolean get() = openedState.value && (touched || query.isNotEmpty())

    fun open() {
        openedState.value = true
        touched = true
        focusRequested = true
    }

    fun edit(value: String) {
        touched = true
        onQueryChange(value)
        // Крестик: строка пуста — фокус остаётся в поле, можно сразу набирать заново.
        if (value.isEmpty()) focusRequested = true
    }

    fun close() {
        openedState.value = false
        touched = false
        focusRequested = false
        focusManager.clearFocus()
        keyboard?.hide()
        onQueryChange("")
    }

    fun applyFocusRequest() {
        if (!focusRequested) return
        focusRequested = false
        focusRequester.requestFocus()
        keyboard?.show()
    }
}

@Composable
internal fun rememberCatalogSearchState(
    query: String,
    onQueryChange: (String) -> Unit,
): CatalogSearchState {
    val opened = rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val state = remember { CatalogSearchState(opened, focusManager, keyboard) }
    state.query = query
    state.onQueryChange = onQueryChange
    val visible = state.visible
    LaunchedEffect(state.focusRequested, visible) { if (visible) state.applyFocusRequest() }
    BackHandler(enabled = visible, onBack = state::close)
    // Уход в тред: поле исчезает, клавиатура не должна оставаться висеть.
    DisposableEffect(state) { onDispose { keyboard?.hide() } }
    return state
}
