package com.solthariel.pawchan.composer

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalComposerImporting = staticCompositionLocalOf { false }

/** Import blocks mutations, not the established appearance of the comment. */
@Composable
internal fun composerCommentColors(preserveAppearance: Boolean) =
    if (preserveAppearance) {
        val scheme = MaterialTheme.colorScheme
        OutlinedTextFieldDefaults.colors(
            disabledTextColor = scheme.onSurface,
            disabledBorderColor = scheme.outline,
            disabledLabelColor = scheme.onSurfaceVariant,
            disabledPlaceholderColor = scheme.onSurfaceVariant,
            disabledSupportingTextColor = scheme.onSurfaceVariant,
        )
    } else {
        OutlinedTextFieldDefaults.colors()
    }
