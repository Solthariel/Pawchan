package com.solthariel.pawchan.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.solthariel.pawchan.feature.composer.R

internal fun showCaptchaSelection(
    captcha: ComposerCaptchaState,
    status: ComposerScreenStatus,
): Boolean =
    captcha.selectedImages.isNotEmpty() && !captcha.satisfied && !status.progress.sent && !status.progress.submitting &&
        !status.progress.captchaLoading && !status.progress.captchaSelecting && status.failure.error == null &&
        status.failure.reason.isNullOrBlank()

@Composable
internal fun CaptchaSelectedSteps(
    images: List<ByteArray>,
    loader: ImageLoader,
    modifier: Modifier,
) {
    val description = stringResource(R.string.captcha_selected_steps, images.size)
    Row(
        modifier.horizontalScroll(rememberScrollState()).clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(4.dp, androidx.compose.ui.Alignment.Start),
    ) {
        images.forEach { image ->
            AsyncImage(
                image,
                null,
                loader,
                Modifier.size(24.dp).background(Color.White, RoundedCornerShape(4.dp)).padding(2.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
