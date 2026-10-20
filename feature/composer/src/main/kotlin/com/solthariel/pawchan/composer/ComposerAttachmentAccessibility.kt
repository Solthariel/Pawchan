package com.solthariel.pawchan.composer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.solthariel.pawchan.feature.composer.R

/** TalkBack can reorder without the long-press gesture. */
@Composable
internal fun Modifier.composerAttachmentOrderSemantics(
    index: Int,
    files: List<ComposerAttachment>,
    enabled: Boolean,
    onChange: (List<ComposerAttachment>) -> Unit,
): Modifier {
    val before = stringResource(R.string.attachment_move_before)
    val after = stringResource(R.string.attachment_move_after)
    val position = stringResource(R.string.attachment_position, index + 1, files.size)
    return this.semantics(mergeDescendants = true) {
        stateDescription = position
        if (enabled) {
            customActions =
                buildList {
                    if (index > 0) {
                        add(
                            CustomAccessibilityAction(before) {
                                onChange(moveComposerItem(files, index, index - 1))
                                true
                            },
                        )
                    }
                    if (index < files.lastIndex) {
                        add(
                            CustomAccessibilityAction(after) {
                                onChange(moveComposerItem(files, index, index + 1))
                                true
                            },
                        )
                    }
                }
        }
    }
}
