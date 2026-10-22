package com.solthariel.pawchan.media.video

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.core.media.R
import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.ImageViewerMotion
import com.solthariel.pawchan.media.LocalMediaChromeState
import com.solthariel.pawchan.media.LocalMediaPreferences
import com.solthariel.pawchan.media.LocalViewerAutoPlay
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.MediaBackdrop
import com.solthariel.pawchan.media.MediaChromeState
import com.solthariel.pawchan.media.MediaNeighborPages
import com.solthariel.pawchan.media.MediaToolbar
import com.solthariel.pawchan.media.MediaViewerChrome
import com.solthariel.pawchan.media.MediaViewerMenu
import com.solthariel.pawchan.media.MediaViewerNavigation
import com.solthariel.pawchan.media.MediaViewerResources
import com.solthariel.pawchan.media.metadata.videoMetadataSource
import com.solthariel.pawchan.media.resolvedMediaUrl
import kotlinx.coroutines.delay

@Composable
internal fun VideoViewer(
    video: MediaAttachmentUi,
    loaders: MediaViewerResources,
    onDownload: () -> Unit,
    onMenu: ((MediaViewerMenu) -> Unit)?,
    onClose: () -> Unit,
    navigation: MediaViewerNavigation = MediaViewerNavigation(),
) {
    val localSource =
        resolvedMediaUrl(video.originalUrl)
    val context = LocalContext.current
    // Возврат из PiP подхватывает ту же играющую сессию: без переоткрытия, перемотки и чёрного кадра.
    val adopted = remember(localSource) { VideoPipHandoff.adopt(localSource) }
    // Открытое заранее или недавно покинутое видео берётся из запаса: без нового открытия и чёрной паузы.
    val pooled = remember(localSource) { if (adopted == null) VideoSessionPool.take(localSource) else null }
    val player =
        remember(localSource) {
            adopted ?: pooled?.first ?: ownedVideoSession(
                FfmpegVideoEngine(context.applicationContext, loaders.contentClient, context.cacheDir),
                localSource,
            )
        }
    BindVideoSessionDiagnostics(video, player, videoSessionOrigin(adopted, pooled))
    val posterNavigation = rememberPosterNavigation(player, navigation)
    val status by key(player) { player.status.collectAsStateWithLifecycle() }
    val hasVideoOutput by rememberVideoOutput(player)
    BindVideoPlaybackLifecycle(
        player,
        adopted = adopted != null,
        pooled = pooled?.second,
        autoPlay = LocalViewerAutoPlay.current,
    )
    BindVideoCompletion(player, status)
    BindNextVideoPreload(navigation.nextItem, loaders, ready = status.stage == VideoStage.READY)
    val onPip = rememberVideoPip(video, player, onClose)
    key(player) {
        VideoSurface(
            player = player,
            video = video,
            navigation = posterNavigation,
            state = status,
            hasVideoOutput = hasVideoOutput,
            actions =
                VideoSurfaceActions(
                    imageLoader = loaders.imageLoader,
                    onRetry = {
                        player.retry()
                    },
                    onDownload = onDownload,
                    onMenu = onMenu,
                    onClose = onClose,
                    onPip = onPip,
                ),
        )
    }
}

@Composable
private fun VideoSurface(
    player: VideoPlaybackSession,
    video: MediaAttachmentUi,
    state: VideoPlaybackStatus,
    actions: VideoSurfaceActions,
    navigation: MediaViewerNavigation,
    hasVideoOutput: Boolean,
) {
    val scope = rememberCoroutineScope()
    val localSource =
        resolvedMediaUrl(video.originalUrl)
    val context = LocalContext.current
    val chrome = LocalMediaChromeState.current ?: remember { MediaChromeState() }
    val motion = remember(player, video.id, video.source.ownerPostId) { ImageViewerMotion(scope, context, chrome) }
    DisposableEffect(motion) { onDispose { motion.stop() } }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clipToBounds()
                .graphicsLayer { alpha = motion.windowAlpha },
    ) {
        MediaBackdrop(motion)
        MediaNeighborPages(navigation, actions.imageLoader, motion)
        // Как в Dashchan: при скрытых панелях «Обновить» появляется в самом меню.
        val metadata = remember(player, video.byteSize) { videoMetadataSource(player, video.byteSize) }
        val menu = { anchored: Boolean ->
            val hidden = !motion.controlsVisible
            actions.onMenu?.invoke(
                MediaViewerMenu(
                    onRefresh = actions.onRetry.takeIf { hidden },
                    softwareDecoder = state.software,
                    onSoftwareDecoder = { player.setSoftware(!state.software) },
                    metadata = metadata,
                    anchored = anchored,
                ),
            )
            Unit
        }
        VideoGestureSurface(player, video, motion, navigation, actions.onClose) { menu(false) }
        VideoPosterLayer(player, video, actions.imageLoader, motion, hasVideoOutput)
        VideoStateOverlay(state, actions.onRetry)
        MediaViewerChrome(
            motion,
            // Ошибка загрузки не убирает кнопки, время и переход к соседним вложениям.
            bottom = { VideoPlaybackPanel(player, navigation, actions.onPip) },
        ) {
            MediaToolbar(
                video,
                navigation,
                actions.onClose,
                actions.onDownload,
                actions.onRetry,
                onMore = actions.onMenu?.let { { menu(true) } },
            )
        }
    }
}

private data class VideoSurfaceActions(
    val imageLoader: coil3.ImageLoader,
    val onRetry: () -> Unit,
    val onDownload: () -> Unit,
    val onMenu: ((MediaViewerMenu) -> Unit)?,
    val onClose: () -> Unit,
    val onPip: (() -> Unit)?,
)

@Composable
private fun VideoStateOverlay(
    state: VideoPlaybackStatus,
    onRetry: () -> Unit,
) {
    when (state.stage) {
        VideoStage.BUFFERING -> {
            Unit
        }

        VideoStage.ERROR -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.video_load_failed),
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

        VideoStage.READY -> {
            Unit
        }
    }
}

/** Следующее видео открывается, когда текущее уже готово: оно не отнимает сеть у того, что смотрят. */
@Composable
private fun BindNextVideoPreload(
    next: MediaAttachmentUi?,
    loaders: MediaViewerResources,
    ready: Boolean,
) {
    val context = LocalContext.current.applicationContext
    val enabled = LocalMediaPreferences.current.preloadVideo
    val url = next?.takeIf { it.viewer == AttachmentViewer.VIDEO }?.let { resolvedMediaUrl(it.originalUrl) }
    LaunchedEffect(url, ready, enabled) {
        if (url == null || !ready || !enabled) return@LaunchedEffect
        delay(PRELOAD_DELAY_MS)
        VideoSessionPool.preload(url) {
            ownedVideoSession(FfmpegVideoEngine(context, loaders.contentClient, context.cacheDir), url)
        }
    }
}

private const val PRELOAD_DELAY_MS = 500L

private fun videoSessionOrigin(
    adopted: VideoPlaybackSession?,
    pooled: Pair<VideoPlaybackSession, Boolean>?,
): String =
    when {
        adopted != null -> "pip"
        pooled != null -> "pool"
        else -> "new"
    }
