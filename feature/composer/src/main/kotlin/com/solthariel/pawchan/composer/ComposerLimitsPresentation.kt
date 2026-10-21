package com.solthariel.pawchan.composer

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.solthariel.pawchan.feature.composer.R

@Composable
internal fun composerLimitsText(
    limits: ComposerLimits,
    commentLength: Int,
): AnnotatedString {
    val remaining = limits.remainingUploadBytes
    val number = remaining?.let(::remainingUploadMegabytes) ?: "…"
    val text =
        stringResource(R.string.composer_limits, number, limits.maxFiles, limits.maxCommentLength - commentLength)
    return buildAnnotatedString {
        if (remaining != null && remaining < 0) {
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.error)) { append(number) }
            append(text.removePrefix(number))
        } else {
            append(text)
        }
    }
}
