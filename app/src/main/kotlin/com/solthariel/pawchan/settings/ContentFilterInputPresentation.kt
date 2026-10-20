package com.solthariel.pawchan.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import com.solthariel.pawchan.domain.filter.ContentFilterMode

internal data class ContentFilterInputPresentation(
    val hint: String = "",
    val error: String? = null,
    val preview: AnnotatedString? = null,
)

internal fun filterConditionPresentation(mode: String): ContentFilterInputPresentation =
    ContentFilterInputPresentation(
        hint =
            if (mode == ContentFilterMode.ALL_WORDS.name) {
                "Слова через пробел или запятую. Порядок не важен"
            } else {
                "Введите фразу целиком"
            },
        error = null,
    )

@Composable
internal fun FilterInputHint(
    presentation: ContentFilterInputPresentation,
    modifier: Modifier = Modifier,
) {
    Text(
        presentation.error?.let { "$it. ${presentation.hint}" } ?: presentation.hint,
        modifier = modifier,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodyMedium,
        color =
            if (presentation.error != null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
    )
}

@Composable
internal fun filterInputColor(
    focused: Boolean,
    error: Boolean,
): Color =
    when {
        error -> MaterialTheme.colorScheme.error
        focused -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

internal fun filterEmptyErrorIsInline(
    error: String,
    text: String,
): Boolean = text.isBlank() && (error == "Условие пустое" || error == "Введите одну точную фразу")
