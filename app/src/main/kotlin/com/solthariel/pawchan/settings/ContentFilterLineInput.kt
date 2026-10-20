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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
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
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FilterTextInput(label, value, onChange, presentation, singleLine, filterInputMinimumHeight())
    }
}

@Composable
private fun FilterTextInput(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    presentation: ContentFilterInputPresentation,
    singleLine: Boolean,
    minimumHeight: Dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val baseStyle = MaterialTheme.typography.bodyLarge
    val lineHeight = with(LocalDensity.current) { maxOf(baseStyle.lineHeight.toPx(), 24.dp.toPx()).toSp() }
    val preview = presentation.preview
    val transformation = remember(preview) { VisualTransformation { filterInputPreview(it, preview) } }
    Column {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = minimumHeight)
                    .padding(vertical = 8.dp)
                    .semantics { contentDescription = label },
            singleLine = singleLine,
            minLines = 1,
            maxLines = if (singleLine) 1 else 3,
            textStyle =
                MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = lineHeight,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
                ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            interactionSource = interaction,
            visualTransformation = transformation,
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    inner()
                    if (value.isBlank() && presentation.hint.isNotEmpty()) {
                        FilterInputHint(presentation)
                    }
                }
            },
        )
        HorizontalDivider(
            thickness = 1.dp,
            color = filterInputColor(focused, presentation.error != null),
        )
    }
}

internal fun filterInputPreview(
    input: AnnotatedString,
    preview: AnnotatedString?,
): TransformedText = TransformedText(preview?.takeIf { it.text == input.text } ?: input, OffsetMapping.Identity)
