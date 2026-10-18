package com.solthariel.pawchan.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import com.solthariel.pawchan.core.media.R
import com.solthariel.pawchan.media.metadata.imageMetadataSource

@Composable
internal fun ImageViewer(
    image: MediaAttachmentUi,
    imageLoader: coil3.ImageLoader,
    onDownload: () -> Unit,
    onMenu: ((MediaViewerMenu) -> Unit)?,
    onClose: () -> Unit,
    navigation: MediaViewerNavigation = MediaViewerNavigation(),
) {
    var retryToken by remember(image.originalUrl) { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val chrome = LocalMediaChromeState.current ?: remember { MediaChromeState() }
    val motion =
        remember(image.originalUrl, image.id, image.source.ownerPostId) {
            ImageViewerMotion(scope, context, chrome)
        }
    DisposableEffect(motion) { onDispose { motion.stop() } }
    val url = resolvedMediaUrl(image.originalUrl)
    val request = rememberMediaImageRequest(url, retryToken)
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clipToBounds()
                .graphicsLayer { alpha = motion.windowAlpha },
    ) {
        val refresh = {
            motion.reset()
            retryToken += 1
        }
        MediaBackdrop(motion)
        MediaNeighborPages(navigation, imageLoader, motion)
        ViewerImage(
            model =
                ViewerImageModel(
                    request = request,
                    contentDescription = image.fileName,
                    width = image.dimensions?.width ?: 0,
                    height = image.dimensions?.height ?: 0,
                    thumbnailUrl = image.thumbnailUrl,
                ),
            imageLoader = imageLoader,
            motion = motion,
            onRetry = refresh,
        )
        // Как в Dashchan: при скрытых панелях «Обновить» появляется в самом меню.
        val metadata = remember(context, imageLoader, request) { imageMetadataSource(context, imageLoader, url) }
        val menu = { anchored: Boolean ->
            val hidden = !motion.controlsVisible
            val viewer = MediaViewerMenu(onRefresh = refresh.takeIf { hidden }, metadata = metadata)
            onMenu?.invoke(viewer.copy(anchored = anchored))
            Unit
        }
        Box(Modifier.fillMaxSize().mediaViewerGestures(motion, navigation, onClose) { menu(false) })
        MediaViewerChrome(motion) {
            MediaToolbar(
                attachment = image,
                navigation = navigation,
                onClose = onClose,
                onSave = onDownload,
                onRefresh = refresh,
                onMore = onMenu?.let { { menu(true) } },
            )
        }
    }
}

@Composable
private fun ViewerImage(
    model: ViewerImageModel,
    imageLoader: coil3.ImageLoader,
    motion: ImageViewerMotion,
    onRetry: () -> Unit,
) {
    // Миниатюра лежит под оригиналом, пока тот не загрузится: без мигания пустого кадра.
    var settled by remember(model.request) { mutableStateOf(false) }
    if (!settled) MediaLoadingImage(model.thumbnailUrl, imageLoader, Modifier.mediaTransform(motion))
    SubcomposeAsyncImage(
        model = model.request,
        imageLoader = imageLoader,
        contentDescription = model.contentDescription,
        modifier =
            Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    motion.resize(
                        ImageViewport(
                            size.width.toFloat(),
                            size.height.toFloat(),
                            model.width.takeIf { it > 0 }?.toFloat() ?: size.width.toFloat(),
                            model.height.takeIf { it > 0 }?.toFloat() ?: size.height.toFloat(),
                        ),
                    )
                }.mediaTransform(motion),
        onSuccess = { success ->
            settled = true
            val viewport = motion.viewport
            motion.resize(
                viewport.copy(
                    imageWidth =
                        model.width.takeIf { it > 0 }?.toFloat() ?: success.result.image.width
                            .toFloat(),
                    imageHeight =
                        model.height.takeIf { it > 0 }?.toFloat() ?: success.result.image.height
                            .toFloat(),
                ),
            )
        },
        contentScale = ContentScale.Fit,
        loading = {},
        onError = { settled = true },
        error = { ImageError(onRetry) },
    )
}

private data class ViewerImageModel(
    val request: ImageRequest,
    val contentDescription: String,
    val width: Int,
    val height: Int,
    val thumbnailUrl: String?,
)

@Composable
private fun ImageError(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.image_load_failed),
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Text(stringResource(R.string.retry))
        }
    }
}
