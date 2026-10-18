package com.solthariel.pawchan.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.settings.ComposerField
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

@OptIn(ExperimentalMaterial3Api::class)
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
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text("$label — $state") } },
        state = rememberTooltipState(),
    ) {
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
                if (!enabled) {
                    colors.onSurface.copy(alpha = DISABLED_OPTION_ALPHA)
                } else if (!checked) {
                    colors.onSurface
                } else if (kind == UiIconKind.SAGE) {
                    colors.tertiary
                } else {
                    colors.primary
                },
                Modifier.size(24.dp),
            )
        }
    }
}

private const val DISABLED_OPTION_ALPHA = 0.38f
