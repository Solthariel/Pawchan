package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow

@Composable
internal fun FilterLineInput(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    presentation: ContentFilterInputPresentation = ContentFilterInputPresentation(),
    singleLine: Boolean = false,
) {
    val preview = presentation.preview
    val transformation = remember(preview) { VisualTransformation { filterInputPreview(it, preview) } }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = filterInputMinimumHeight())
                .semantics { contentDescription = label },
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        placeholder = { if (presentation.hint.isNotEmpty()) FilterInputHint(presentation) },
        singleLine = singleLine,
        minLines = 1,
        maxLines = if (singleLine) 1 else 3,
        textStyle = MaterialTheme.typography.bodyLarge,
        visualTransformation = transformation,
        isError = presentation.error != null,
    )
}

internal fun filterInputPreview(
    input: AnnotatedString,
    preview: AnnotatedString?,
): TransformedText = TransformedText(preview?.takeIf { it.text == input.text } ?: input, OffsetMapping.Identity)
