package com.solthariel.pawchan.composer

import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.solthariel.pawchan.core.ui.AppDialogStyle
import com.solthariel.pawchan.media.AttachmentDialogContent
import com.solthariel.pawchan.media.AttachmentDialogLayout

@Composable
internal fun ComposerAttachmentViewer(
    file: ComposerAttachment,
    imageLoader: ImageLoader,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onUpdate: (ComposerAttachment) -> Unit,
    onRemove: () -> Unit,
) {
    AttachmentFixedDialog(onDismiss) {
        Surface(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            color = MaterialTheme.colorScheme.background,
            shape = AppDialogStyle.shape,
        ) {
            AttachmentDialogLayout(
                AttachmentDialogContent(
                    header = { Box(Modifier.fillMaxWidth()) },
                    footer = {
                        AttachmentEditorFooter(
                            file,
                            enabled,
                            onUpdate,
                            onRemove,
                        )
                    },
                    media = { ComposerViewerMedia(file, imageLoader) },
                    aspectRatio = file.aspectRatio,
                    previewHeightLimit = 240.dp,
                ),
                Modifier.padding(12.dp),
            )
        }
    }
}

@Composable
private fun ComposerViewerMedia(
    file: ComposerAttachment,
    imageLoader: ImageLoader,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (file.video && file.sourceUri != null) {
            ComposerLocalVideo(file.sourceUri)
        } else {
            AsyncImage(
                model = file.source.content,
                imageLoader = imageLoader,
                contentDescription = file.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun ComposerLocalVideo(uri: String) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val video =
        remember(uri, context, lifecycle) {
            VideoView(context).apply {
                val controls = MediaController(context)
                controls.setAnchorView(this)
                setMediaController(controls)
                setOnPreparedListener {
                    // Render the initial frame without autoplaying in attachment settings.
                    seekTo(1)
                    pause()
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) controls.show()
                }
                setVideoURI(uri.toUri())
            }
        }
    DisposableEffect(video, lifecycle) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) video.pause()
            }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            video.stopPlayback()
        }
    }
    AndroidView(factory = { video }, modifier = Modifier.fillMaxSize(), onRelease = { it.stopPlayback() })
}
