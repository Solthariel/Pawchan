package com.solthariel.pawchan.settings

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
internal fun FilterLineInput(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    presentation: ContentFilterInputPresentation = ContentFilterInputPresentation(),
    singleLine: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val preview = presentation.preview
    val transformation = remember(preview) { VisualTransformation { filterInputPreview(it, preview) } }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 36.dp)
                    .padding(vertical = 8.dp)
                    .semantics { contentDescription = label },
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else 3,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            interactionSource = interaction,
            visualTransformation = transformation,
            decorationBox = { inner ->
                Box {
                    if (value.isBlank()) FilterInputHint(presentation)
                    inner()
                }
            },
        )
        HorizontalDivider(
            thickness = if (focused) 2.dp else 1.dp,
            color = filterInputColor(focused, presentation.error != null),
        )
    }
}

internal fun filterInputPreview(
    input: AnnotatedString,
    preview: AnnotatedString?,
): TransformedText = TransformedText(preview?.takeIf { it.text == input.text } ?: input, OffsetMapping.Identity)
