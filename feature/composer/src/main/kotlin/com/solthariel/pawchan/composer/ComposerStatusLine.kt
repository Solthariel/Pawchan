package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.feature.composer.R

@Composable
internal fun ComposerStatusLine(
    captcha: ComposerCaptchaState,
    status: ComposerScreenStatus,
    onReload: () -> Unit,
    imageLoader: coil3.ImageLoader,
) {
    val remaining = rememberCaptchaRemainingSeconds(captcha)
    val expired = remaining == 0L
    val reloadLabel = stringResource(R.string.captcha_retry)
    // Статус по центру строки: слева пустое место той же ширины, что таймер с кнопкой справа.
    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Spacer(Modifier.width(STATUS_SIDE_WIDTH))
        ComposerStatusBody(captcha, status, imageLoader, expired)
        Row(
            // A multiline failure must not push the reload button below the visible viewport.
            Modifier.width(STATUS_SIDE_WIDTH).align(androidx.compose.ui.Alignment.Top),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            ComposerStatusControls(remaining, expired, reloadLabel, status, onReload)
        }
    }
}

@Composable
private fun RowScope.ComposerStatusBody(
    captcha: ComposerCaptchaState,
    status: ComposerScreenStatus,
    imageLoader: coil3.ImageLoader,
    expired: Boolean,
) {
    val idle = !status.progress.submitting && !status.progress.sent
    val captchaIdle = idle && status.uploadBudget?.status == null
    val display =
        composerStatusText(
            status.progress,
            status.failure,
            captcha.satisfied,
            stringResource(R.string.captcha_solved),
        ).withUploadBudget(status.uploadBudget, status.progress)
    if (!expired && status.uploadBudget?.status == null && showCaptchaSelection(captcha, status)) {
        Box(Modifier.weight(1f), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CaptchaSelectedSteps(captcha.selectedImages, imageLoader, Modifier)
        }
    } else {
        Text(
            if (expired && !display.failed && captchaIdle) {
                stringResource(R.string.captcha_expired)
            } else {
                display.label
            },
            Modifier.weight(1f),
            maxLines = if (display.failed) Int.MAX_VALUE else 2,
            textAlign = TextAlign.Center,
            color = if (display.failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ComposerStatusControls(
    remaining: Long?,
    expired: Boolean,
    reloadLabel: String,
    status: ComposerScreenStatus,
    onReload: () -> Unit,
) {
    if (remaining != null) {
        Text(
            captchaTimerLabel(remaining),
            Modifier.padding(start = 6.dp).semantics {
                contentDescription = "Осталось $remaining секунд"
            },
            color = if (expired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    IconButton(
        onClick = onReload,
        modifier = Modifier.size(48.dp).semantics { contentDescription = reloadLabel },
        enabled =
            !status.progress.submitting && !status.progress.sent &&
                !status.progress.captchaLoading && !status.progress.captchaSelecting,
    ) {
        UiIcon(UiIconKind.REFRESH, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(24.dp))
    }
}

private val STATUS_SIDE_WIDTH = 100.dp
