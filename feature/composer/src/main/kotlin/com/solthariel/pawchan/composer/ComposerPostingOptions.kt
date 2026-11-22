package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.feature.composer.R

@Composable
internal fun ComposerPostingOptions(
    draft: ComposerDraftState,
    rules: PostingRules?,
    enabled: Boolean,
    onChange: (ComposerDraftState) -> Unit,
) {
    Row {
        if (rules?.sageEnabled == true) {
            ComposerPostToggle(UiIconKind.SAGE, stringResource(R.string.composer_sage), draft.options.sage, enabled) {
                onChange(draft.copy(options = draft.options.copy(sage = it)))
            }
        }
        run {
            ComposerPostToggle(
                UiIconKind.PERSON,
                stringResource(R.string.composer_thread_author),
                draft.options.threadAuthor,
                enabled,
            ) {
                onChange(draft.copy(options = draft.options.copy(threadAuthor = it)))
            }
        }
    }
}

@Composable
private fun ComposerPostToggle(
    kind: UiIconKind,
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val state = stringResource(if (checked) R.string.composer_option_on else R.string.composer_option_off)
    IconToggleButton(
        checked,
        onChange,
        enabled = enabled,
        modifier =
            Modifier.size(48.dp).semantics {
                contentDescription = label
                stateDescription = state
            },
    ) {
        UiIcon(
            kind,
            composerOptionTint(colors, checked, enabled || LocalComposerImporting.current),
            Modifier.size(24.dp),
        )
    }
}

/** Matches selected and unselected bottom navigation icons without adding a selection background. */
internal fun composerOptionTint(
    colors: ColorScheme,
    checked: Boolean,
    enabled: Boolean,
): Color =
    when {
        !enabled -> colors.onSurface.copy(alpha = DISABLED_OPTION_ALPHA)
        checked -> colors.primary
        else -> colors.onSurfaceVariant.copy(alpha = UNSELECTED_OPTION_ALPHA)
    }

private const val UNSELECTED_OPTION_ALPHA = 0.55f
private const val DISABLED_OPTION_ALPHA = 0.38f
