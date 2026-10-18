package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.settings.ComposerField
import com.solthariel.pawchan.feature.composer.R

@Composable
internal fun ComposerIdentityFields(
    draft: ComposerDraftState,
    rules: PostingRules?,
    enabled: Boolean,
    onChange: (ComposerDraftState) -> Unit,
    showIconSelector: Boolean = true,
) {
    val hidden = LocalComposerPreferences.current.hiddenFields
    if (rules?.namesEnabled == true && ComposerField.NAME !in hidden) {
        OutlinedTextField(
            value = draft.options.name,
            onValueChange = { onChange(draft.copy(options = draft.options.copy(name = it))) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.composer_name)) },
            singleLine = true,
            enabled = enabled,
        )
    }
    if (showIconSelector) {
        ComposerIconSelector(draft, rules, enabled, onChange)
    }
    if (ComposerField.EMAIL !in hidden) {
        OutlinedTextField(
            value = draft.email,
            onValueChange = { onChange(draft.copy(email = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.composer_email)) },
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
    }
}
