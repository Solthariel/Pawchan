package com.solthariel.pawchan.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.domain.model.MediaDimensions
import com.solthariel.pawchan.domain.model.MediaKind
import com.solthariel.pawchan.media.video.BindVideoPlaybackLifecycle
import com.solthariel.pawchan.media.video.MpvVideoEngine
import com.solthariel.pawchan.media.video.VideoPlaybackPanel
import com.solthariel.pawchan.media.video.VideoPlaybackStatus
import com.solthariel.pawchan.media.video.VideoRenderSurface
import com.solthariel.pawchan.media.video.VideoStage
import com.solthariel.pawchan.media.video.VideoStateOverlay
import com.solthariel.pawchan.media.video.ownedVideoSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Uses the post video backend and playback panel in the attachment editor. */
@Composable
fun LocalVideoAttachmentPreview(
    id: String,
    fileName: String,
    content: ByteArray,
    dimensions: String? = null,
) {
    val directory = LocalContext.current.cacheDir
    var file by remember(id, content) { mutableStateOf<File?>(null) }
    var error by remember(id, content) { mutableStateOf(false) }
    var attempt by remember(id, content) { mutableIntStateOf(0) }
    LaunchedEffect(id, content, directory, attempt) {
        var temporary: File? = null
        file = null
        error = false
        try {
            withContext(Dispatchers.IO) {
                temporary = File.createTempFile(PREVIEW_FILE_PREFIX, ".media", directory)
                requireNotNull(temporary).writeBytes(content)
            }
            file = temporary
            awaitCancellation()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            error = true
        } finally {
            withContext(NonCancellable + Dispatchers.IO) { temporary?.delete() }
        }
    }
    val current = file
    if (current != null) {
        LocalVideoPreview(current, id, fileName, content.size.toLong(), dimensions)
    } else if (error) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            VideoStateOverlay(VideoPlaybackStatus(stage = VideoStage.ERROR)) { attempt++ }
        }
    }
}

@Composable
private fun LocalVideoPreview(
    file: File,
    id: String,
    fileName: String,
    byteSize: Long,
    dimensions: String?,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val url = remember(file) { file.toURI().toString() }
    val video =
        remember(id, fileName, url, byteSize, dimensions) {
            MediaAttachmentUi(
                source = MediaAttachmentSource(id, fileName, url, thumbnailUrl = null),
                byteSize = byteSize,
                viewer = AttachmentViewer.VIDEO,
                kind = MediaKind.VIDEO,
                dimensions = localVideoDimensions(dimensions),
            )
        }
    val player =
        remember(url) {
            ownedVideoSession(MpvVideoEngine(context.applicationContext, null, context.cacheDir), url)
        }
    val motion = remember(player) { ImageViewerMotion(scope, context) }
    val status by player.status.collectAsStateWithLifecycle()
    DisposableEffect(motion) { onDispose { motion.stop() } }
    BindVideoPlaybackLifecycle(player, autoPlay = false, retainOnDispose = false, initialPosition = 1L)
    val seekColors = MaterialTheme.colorScheme
    MediaRotationScope {
        Column(Modifier.fillMaxSize().background(Color.Black)) {
            Box(Modifier.weight(1f)) {
                VideoRenderSurface(player, video, motion)
                VideoStateOverlay(status, player::retry)
            }
            MaterialTheme(colorScheme = MEDIA_CHROME_COLORS) { VideoPlaybackPanel(player, seekColors = seekColors) }
        }
    }
}

private const val PREVIEW_FILE_PREFIX = "composer-video-"

/** Untrusted attachment metadata is only a fallback until mpv reports its display aspect. */
internal fun localVideoDimensions(value: String?): MediaDimensions? {
    val sides = value?.split('×')
    return if (sides == null || sides.size != 2) {
        null
    } else {
        MediaDimensions.create(sides[0].trim().toIntOrNull(), sides[1].trim().toIntOrNull())
    }
}
