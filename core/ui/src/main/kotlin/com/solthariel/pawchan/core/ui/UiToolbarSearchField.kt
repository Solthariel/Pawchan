package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Shared inline Material search; screen owners retain focus, query and Back lifecycle. */
@Composable
fun UiToolbarSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier,
    placeholder: String = "Поиск",
    clearDescription: String = "Очистить поиск",
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier =
            modifier
                .heightIn(min = 56.dp)
                .focusRequester(focusRequester)
                .semantics { contentDescription = placeholder },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = MaterialTheme.shapes.extraLarge,
        placeholder = { Text(placeholder, maxLines = 1) },
        trailingIcon =
            if (query.isEmpty()) {
                null
            } else {
                { UiToolbarButton(UiIconKind.CLOSE, clearDescription, onClick = { onQueryChange("") }) }
            },
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
    )
}
