package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** The same inline search geometry is shared by the catalog and filter list. */
@Composable
fun UiToolbarSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier,
    placeholder: String = "Поиск",
    clearDescription: String = "Очистить поиск",
) {
    Row(
        modifier
            .height(34.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = SEARCH_ALPHA), RoundedCornerShape(17.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UiIcon(UiIconKind.SEARCH, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(20.dp))
        Spacer(Modifier.size(10.dp))
        ToolbarSearchInput(query, onQueryChange, focusRequester, placeholder, Modifier.weight(1f))
        if (query.isNotEmpty()) {
            UiToolbarButton(UiIconKind.CLOSE, clearDescription, onClick = { onQueryChange("") })
        }
    }
}

@Composable
private fun ToolbarSearchInput(
    query: String,
    onChange: (String) -> Unit,
    focus: FocusRequester,
    placeholder: String,
    modifier: Modifier,
) {
    BasicTextField(
        value = query,
        onValueChange = onChange,
        modifier = modifier.focusRequester(focus).semantics { contentDescription = placeholder },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { inner ->
            if (query.isEmpty()) {
                Text(
                    placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            inner()
        },
    )
}

private const val SEARCH_ALPHA = 0.58f
