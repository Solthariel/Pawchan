package com.solthariel.pawchan.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solthariel.pawchan.feature.composer.R

@Composable
internal fun ComposerFormattingToolbar(
    enabled: Boolean,
    filesLoading: Boolean,
    onClear: () -> Unit,
    onMarkup: (CommentMarkup) -> Unit,
) {
    Row(Modifier.fillMaxWidth()) {
        CommentMarkup.entries.forEach { markup ->
            val description = stringResource(markup.descriptionResource)
            ComposerMarkupKey(
                Modifier.weight(1f),
                enabled && !filesLoading,
                description,
                onClick = { onMarkup(markup) },
            ) {
                Text(
                    markup.label,
                    maxLines = 1,
                    fontSize = if (markup.label.length > 1) 11.sp else 16.sp,
                    fontWeight = if (markup == CommentMarkup.BOLD) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (markup == CommentMarkup.ITALIC) FontStyle.Italic else FontStyle.Normal,
                    textDecoration =
                        when (markup) {
                            CommentMarkup.UNDERLINE -> TextDecoration.Underline
                            CommentMarkup.STRIKE -> TextDecoration.LineThrough
                            else -> null
                        },
                )
            }
        }
        ComposerMarkupKey(
            Modifier.weight(1f),
            enabled,
            stringResource(R.string.composer_clear_all),
            onClick = onClear,
        ) {
            Icon(
                painterResource(R.drawable.ic_composer_backspace),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private val CommentMarkup.descriptionResource: Int
    get() =
        when (this) {
            CommentMarkup.BOLD -> R.string.markup_bold
            CommentMarkup.ITALIC -> R.string.markup_italic
            CommentMarkup.UNDERLINE -> R.string.markup_underline
            CommentMarkup.OVERLINE -> R.string.markup_overline
            CommentMarkup.STRIKE -> R.string.markup_strike
            CommentMarkup.SUBSCRIPT -> R.string.markup_sub
            CommentMarkup.SUPERSCRIPT -> R.string.markup_sup
            CommentMarkup.SPOILER -> R.string.markup_spoiler
            CommentMarkup.QUOTE -> R.string.markup_quote
        }

@Composable
private fun ComposerMarkupKey(
    modifier: Modifier,
    enabled: Boolean,
    description: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    BoxWithConstraints(
        modifier
            .height(48.dp)
            .semantics { contentDescription = description }
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(maxWidth.coerceAtMost(48.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(if (enabled && (pressed || focused)) color.copy(alpha = 0.16f) else Color.Transparent)
                .testTag("composer-markup-surface"),
        )
        CompositionLocalProvider(LocalContentColor provides color) { content() }
    }
}
