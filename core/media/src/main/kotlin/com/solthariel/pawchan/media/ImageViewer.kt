package com.solthariel.pawchan.media

import androidx.compose.foundation.Image
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import coil3.compose.AsyncImagePainter
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
    val pages = rememberViewerImagePages(image, navigation, imageLoader, retryToken)
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .then(pages.sizeResolver)
                .clipToBounds()
                .graphicsLayer { alpha = motion.windowAlpha },
    ) {
        val refresh = {
            motion.reset()
            retryToken += 1
        }
        MediaBackdrop(motion)
        MediaNeighborPages(navigation, imageLoader, motion, pages)
        ViewerImage(
            model = image.viewerImageModel(pages),
            imageLoader = imageLoader,
            painter = pages[url],
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
private fun MediaAttachmentUi.viewerImageModel(pages: ViewerImagePages): ViewerImageModel =
    ViewerImageModel(
        originalUrl = originalUrl,
        contentDescription = fileName,
        width = dimensions?.width ?: 0,
        height = dimensions?.height ?: 0,
        thumbnailUrl = thumbnailUrl,
        thumbnailPainter = thumbnailUrl?.let { pages.painters[resolvedMediaUrl(it)] },
    )

@Composable
private fun ViewerImage(
    model: ViewerImageModel,
    imageLoader: coil3.ImageLoader,
    painter: AsyncImagePainter,
    motion: ImageViewerMotion,
    onRetry: () -> Unit,
) {
    val state = key(painter) { painter.state.collectAsState().value }
    var retained by remember(model.originalUrl, imageLoader) { mutableStateOf<AsyncImagePainter.State.Success?>(null) }
    val success = (state as? AsyncImagePainter.State.Success) ?: retained
    SideEffect { if (state is AsyncImagePainter.State.Success) retained = state as AsyncImagePainter.State.Success }
    if (success == null) {
        ViewerImagePlaceholder(model, imageLoader, motion, state is AsyncImagePainter.State.Error, onRetry)
    }
    val result = success?.result
    LaunchedEffect(result, motion) {
        if (result != null) {
            motion.resize(
                motion.viewport.copy(
                    imageWidth = model.width.takeIf { it > 0 }?.toFloat() ?: result.image.width.toFloat(),
                    imageHeight = model.height.takeIf { it > 0 }?.toFloat() ?: result.image.height.toFloat(),
                ),
            )
        }
    }
    Image(
        painter = success?.painter ?: painter,
        contentDescription = model.contentDescription,
        modifier =
            Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    motion.resize(
                        ImageViewport(
                            size.width.toFloat(),
                            size.height.toFloat(),
                            model.width.takeIf { it > 0 }?.toFloat()
                                ?: result?.image?.width?.toFloat() ?: size.width.toFloat(),
                            model.height.takeIf { it > 0 }?.toFloat()
                                ?: result?.image?.height?.toFloat() ?: size.height.toFloat(),
                        ),
                    )
                }.mediaTransform(motion),
        contentScale = ContentScale.Fit,
    )
}

private data class ViewerImageModel(
    val originalUrl: String,
    val contentDescription: String,
    val width: Int,
    val height: Int,
    val thumbnailUrl: String?,
    val thumbnailPainter: AsyncImagePainter?,
)

@Composable
private fun ViewerImagePlaceholder(
    model: ViewerImageModel,
    imageLoader: coil3.ImageLoader,
    motion: ImageViewerMotion,
    originalFailed: Boolean,
    onRetry: () -> Unit,
) {
    var previewFailed by remember(model.originalUrl, model.thumbnailUrl, imageLoader) { mutableStateOf(false) }
    val preview = model.thumbnailPainter
    val previewState = key(preview) { preview?.state?.collectAsState()?.value }
    if (preview != null && previewState is AsyncImagePainter.State.Success) {
        Image(preview, null, Modifier.fillMaxSize().mediaTransform(motion), contentScale = ContentScale.Fit)
    } else {
        MediaLoadingImage(
            model.thumbnailUrl?.let { resolvedMediaUrl(it) },
            imageLoader,
            Modifier.mediaTransform(motion),
            onState = { previewFailed = it == "coil_failed" || it == "missing_url" },
        )
    }
    if (originalFailed && previewState !is AsyncImagePainter.State.Success && previewFailed) ImageError(onRetry)
}

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
