package com.solthariel.pawchan.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
            when (mode) {
                ContentFilterMode.ANY_TERM.name, ContentFilterMode.ALL_TERMS.name -> FILTER_CONDITION_HINTS[1]
                else -> FILTER_CONDITION_HINTS[0]
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
        maxLines = 1,
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

internal fun filterEmptyErrorIsInline(
    error: String,
    text: String,
): Boolean = text.isBlank() && (error == "Условие пустое" || error == "Введите одну фразу")

private val FILTER_CONDITION_HINTS =
    listOf(
        "Введите фразу целиком",
        "По одному слову или фразе на строку",
    )
