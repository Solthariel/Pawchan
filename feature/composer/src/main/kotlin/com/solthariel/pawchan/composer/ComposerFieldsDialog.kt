package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.domain.posting.NewThreadDraft
import com.solthariel.pawchan.domain.settings.ComposerField
import com.solthariel.pawchan.feature.composer.R
import com.solthariel.pawchan.feature.composer.R.string as L

@Composable
internal fun ComposerFieldsDialog(
    state: ComposerScreenState,
    fields: List<ComposerField>,
    onChange: (ComposerDraftState) -> Unit,
    onDismiss: () -> Unit,
) {
    val res = LocalResources.current

    ComposerModal(onDismiss) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                res.getString(L.ui_additional_fields),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            fields.forEach { ComposerExtraField(it, state.draft, !state.draft.inputBlocked, onChange) }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(res.getString(L.ui_done)) }
        }
    }
}

@Composable
private fun ComposerExtraField(
    field: ComposerField,
    draft: ComposerDraftState,
    enabled: Boolean,
    onChange: (ComposerDraftState) -> Unit,
) {
    val res = LocalResources.current

    val label =
        stringResource(
            when (field) {
                ComposerField.NAME -> R.string.composer_name
                ComposerField.EMAIL -> R.string.composer_email
                ComposerField.SUBJECT -> R.string.new_thread_subject
                ComposerField.TAG -> R.string.composer_tag
            },
        )
    val value = draft.fieldValue(field)
    val limit =
        when (field) {
            ComposerField.SUBJECT -> NewThreadDraft.MAX_SUBJECT_LENGTH
            ComposerField.TAG -> NewThreadDraft.MAX_TAG_LENGTH
            else -> null
        }
    val keyboard = if (field == ComposerField.EMAIL) KeyboardType.Email else KeyboardType.Text
    OutlinedTextField(
        value = value,
        onValueChange = {
            if (field != ComposerField.TAG || it.length <= NewThreadDraft.MAX_TAG_LENGTH) {
                onChange(draft.withFieldValue(field, it))
            }
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onChange(draft.withFieldValue(field, "")) },
                    enabled = enabled,
                    modifier = Modifier.semantics { contentDescription = res.getString(L.ui_clear_994a, label) },
                ) { UiIcon(UiIconKind.DELETE, MaterialTheme.colorScheme.onSurface, Modifier.size(24.dp)) }
            }
        },
        supportingText =
            if (limit != null) {
                {
                    Text(
                        stringResource(R.string.reply_length, value.length, limit),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                }
            } else {
                null
            },
        isError = limit != null && value.length > limit,
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
    )
}
