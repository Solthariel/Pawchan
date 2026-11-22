package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_HORIZONTAL_INSET
import com.solthariel.pawchan.core.ui.APP_TOP_BAR_HEIGHT
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton
import com.solthariel.pawchan.feature.composer.R

@Composable
internal fun ComposerScreenHeader(
    title: String,
    state: ComposerScreenState,
    enabled: Boolean,
    onClose: () -> Unit,
    onChange: (ComposerDraftState) -> Unit,
    fields: ComposerFieldsAccess? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = APP_TOP_BAR_HEIGHT)
            .padding(horizontal = APP_TOOLBAR_HORIZONTAL_INSET),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UiToolbarButton(UiIconKind.BACK, stringResource(R.string.board_dialog_close), onClick = onClose)
        Text(
            title,
            Modifier.weight(1f).padding(start = 12.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleLarge,
        )
        ComposerHeaderOptions(state, enabled, onChange)
        fields?.let { ComposerFieldsButton(it, enabled && !state.draft.inputBlocked) }
    }
}

@Composable
private fun ComposerHeaderOptions(
    state: ComposerScreenState,
    enabled: Boolean,
    onChange: (ComposerDraftState) -> Unit,
) {
    ComposerIconSelector(state.draft, state.rules, enabled, onChange, compact = true)
    ComposerPostingOptions(state.draft, state.rules, enabled && !state.draft.inputBlocked, onChange)
}
