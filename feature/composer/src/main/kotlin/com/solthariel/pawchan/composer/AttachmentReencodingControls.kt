package com.solthariel.pawchan.composer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.solthariel.pawchan.core.ui.ImageQualityControl
import com.solthariel.pawchan.core.ui.UiChoiceSegments
import com.solthariel.pawchan.core.ui.UiOptionRow
import com.solthariel.pawchan.feature.composer.R

@Composable
internal fun AttachmentReencodingControls(
    settings: ImageReencoding,
    enabled: Boolean,
    onChange: (ImageReencoding) -> Unit,
) {
    UiOptionRow("Формат") {
        UiChoiceSegments(listOf("JPEG", "PNG"), if (settings.jpeg) 0 else 1, enabled) {
            onChange(settings.copy(jpeg = it == 0))
        }
    }
    if (settings.jpeg) {
        ImageQualityControl(settings.quality, enabled) { onChange(settings.copy(quality = it)) }
    }
    UiOptionRow(stringResource(R.string.attachment_reduce)) {
        val divisors = listOf(1, HALF_SIZE_DIVISOR, QUARTER_SIZE_DIVISOR, EIGHTH_SIZE_DIVISOR)
        UiChoiceSegments(
            divisors.map { stringResource(R.string.attachment_reduce_factor, it) },
            divisors.indexOfFirst { settings.scaleFactor == 1f / it },
            enabled,
        ) { onChange(settings.copy(scalePercent = 100, scaleDivisor = divisors[it])) }
    }
}

private const val HALF_SIZE_DIVISOR = 2
private const val QUARTER_SIZE_DIVISOR = 4
private const val EIGHTH_SIZE_DIVISOR = 8
