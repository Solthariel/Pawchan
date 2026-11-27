package com.solthariel.pawchan.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import com.solthariel.pawchan.core.ui.TransientNotice

internal data class ContentFilterInputPresentation(
    val hint: String = "",
    val error: String? = null,
    val preview: AnnotatedString? = null,
)

@Composable
internal fun FilterInputHint(
    presentation: ContentFilterInputPresentation,
    modifier: Modifier = Modifier,
) {
    TransientNotice(presentation.error)
    Text(
        presentation.hint,
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
