package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        CommentMarkup.entries.forEach { markup ->
            val description = stringResource(markup.descriptionResource)
            ComposerMarkupKey(
                Modifier.width(48.dp),
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
            Modifier.width(48.dp),
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
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier
                .height(48.dp)
                .testTag("composer-markup-surface")
                .semantics { contentDescription = description },
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { content() }
    }
}
