package com.solthariel.pawchan.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.filter.contentFilterValidationError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ContentFilterValidationState(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val validate: (ContentFilterRule) -> String? = ::contentFilterValidationError,
) {
    private var checked by mutableStateOf<ContentFilterRule?>(null)
    private var error by mutableStateOf<String?>(null)
    var saving by mutableStateOf(false)
        private set

    fun errorFor(rule: ContentFilterRule): String? = error.takeIf { checked == rule }

    fun canSave(draft: ContentFilterDraft): Boolean = draft.text.isNotBlank() && draft.inputError() == null && !saving

    suspend fun check(rule: ContentFilterRule) {
        val result = withContext(dispatcher) { validate(rule) }
        checked = rule
        error = result
    }

    suspend fun save(
        rule: ContentFilterRule,
        inputError: String?,
        currentRule: () -> ContentFilterRule,
        onSave: (ContentFilterRule) -> Unit,
    ) {
        if (saving) return
        saving = true
        try {
            val result = inputError ?: withContext(dispatcher) { validate(rule) }
            checked = rule
            error = result
            if (result == null && currentRule() == rule) onSave(rule)
        } finally {
            saving = false
        }
    }
}
