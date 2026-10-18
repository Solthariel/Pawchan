package com.solthariel.pawchan.media.thumbnail

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale

/** Only a newly delivered image fades; memory hits do not blink on lazy reuse. */
@Composable
internal fun FadingThumbnail(
    bitmap: Bitmap,
    instantly: Boolean,
    description: String?,
    modifier: Modifier,
) {
    val alpha = remember(bitmap) { Animatable(if (instantly) 1f else 0f) }
    LaunchedEffect(bitmap) {
        if (alpha.value < 1f) alpha.animateTo(1f, tween(THUMBNAIL_FADE_MILLIS, easing = ThumbnailFadeEasing))
    }
    val painter =
        remember(bitmap, alpha) {
            ThumbnailFadePainter(BitmapPainter(bitmap.asImageBitmap()), alpha.asState())
        }
    Image(painter, description, modifier, contentScale = ContentScale.Crop)
}

/** Alpha is read in drawing, not composition: no per-frame row recomposition or graphics layer. */
internal class ThumbnailFadePainter(
    private val image: Painter,
    private val opacity: State<Float>,
) : Painter() {
    override val intrinsicSize: Size = image.intrinsicSize

    override fun DrawScope.onDraw() {
        with(image) { draw(size, alpha = opacity.value) }
    }
}
