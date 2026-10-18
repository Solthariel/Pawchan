package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.solthariel.pawchan.domain.posting.EmojiCaptchaOption
import com.solthariel.pawchan.feature.composer.R
import androidx.compose.foundation.lazy.grid.items as gridItems

@Composable
internal fun ComposerCaptcha(
    state: ComposerCaptchaState,
    progress: ComposerProgressState,
    imageLoader: ImageLoader,
    actions: NewThreadCaptchaActions,
) {
    if (!progress.captchaLoading && state.challenge != null) {
        ComposerEmojiChallenge(ComposerCaptchaPresentation(state, progress), imageLoader, actions.onEmojiSelected)
    }
}

@Composable
private fun ComposerEmojiChallenge(
    state: ComposerCaptchaPresentation,
    imageLoader: coil3.ImageLoader,
    onEmojiSelected: (Int) -> Unit,
) {
    val captcha = requireNotNull(state.captcha)
    AsyncImage(
        model = captcha.taskImage,
        imageLoader = imageLoader,
        contentDescription = stringResource(R.string.captcha_image),
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(max = 160.dp),
    )
    CaptchaOptionsGrid(captcha.options, imageLoader, !state.selectionEnabled, onEmojiSelected)
    if (state.captchaSelecting) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CaptchaOptionsGrid(
    options: List<EmojiCaptchaOption>,
    imageLoader: ImageLoader,
    selecting: Boolean,
    onEmojiSelected: (Int) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val rows = (options.size + CAPTCHA_COLUMNS - 1) / CAPTCHA_COLUMNS
        val gap = CAPTCHA_GAP_DP.dp
        val cellSize = (maxWidth - gap * (CAPTCHA_COLUMNS - 1)) / CAPTCHA_COLUMNS
        LazyVerticalGrid(
            columns = GridCells.Fixed(CAPTCHA_COLUMNS),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(cellSize * rows + gap * (rows - 1).coerceAtLeast(0)),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(gap),
            userScrollEnabled = false,
        ) {
            gridItems(
                items = options,
                key = { option -> option.number },
            ) { option ->
                Card(
                    onClick = { onEmojiSelected(option.number) },
                    enabled = !selecting,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                ) {
                    EmojiImage(
                        option.image,
                        imageLoader,
                        stringResource(R.string.captcha_emoji_option, option.number + 1),
                        Modifier.fillMaxSize().padding(3.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmojiImage(
    image: ByteArray,
    imageLoader: coil3.ImageLoader,
    description: String,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = image,
        imageLoader = imageLoader,
        contentDescription = description,
        modifier = modifier,
    )
}

private const val CAPTCHA_COLUMNS = 4
private const val CAPTCHA_GAP_DP = 8
