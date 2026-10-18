package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Centered action lists share width, shape, scrolling and row typography. */
@Composable
fun AppMenuDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    AppDialog(onDismissRequest, modifier) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(8.dp), content = content)
    }
}
