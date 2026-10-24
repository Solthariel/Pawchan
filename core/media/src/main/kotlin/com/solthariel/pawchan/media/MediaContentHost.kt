package com.solthariel.pawchan.media

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.solthariel.pawchan.media.MediaViewerResources
import com.solthariel.pawchan.media.video.LocalVideoPosterStore
import com.solthariel.pawchan.media.video.VideoViewer
import com.solthariel.pawchan.media.video.rememberVideoPosterStore

/**
 * Источник (тред, каталог, галерея, превью) остаётся в композиции под просмотрщиком,
 * поэтому его прокрутка и поля не сбрасываются.
 */
@Composable
internal fun PreservedMediaContent(
    visible: Boolean,
    content: @Composable () -> Unit,
    viewer: @Composable () -> Unit,
) {
    val holder = rememberSaveableStateHolder()
    Box(Modifier.fillMaxSize()) {
        holder.SaveableStateProvider(MEDIA_CONTENT_STATE_KEY) { content() }
        if (!visible) viewer()
    }
}

/**
 * Просмотрщик открывается в собственном окне поверх всех окон экрана
 * (галерея, превью постов, выбор вложений каталога), в отдельном диалоговом окне.
 */
@Composable
fun MediaContentHost(
    selection: MediaViewerSelection,
    mediaLoaders: MediaViewerResources,
    mediaSave: MediaSaveActions,
    onMenu: ((MediaMenuRequest) -> Unit)?,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    val visibleSelection = rememberVisibleMediaSelection(selection)
    val attachment = visibleSelection.attachment
    val session = rememberViewerSession(visibleSelection)
    val navigation = session.navigation
    PreservedMediaContent(
        visible = attachment == null || attachment.viewer == AttachmentViewer.NONE,
        content = content,
        viewer = {
            MediaViewerWindow(onClose) {
                val chrome = remember { MediaChromeState() }
                val posters = rememberVideoPosterStore()
                MediaRotationScope {
                    CompositionLocalProvider(
                        LocalMediaChromeState provides chrome,
                        LocalViewerAutoPlay provides session.autoPlay,
                        LocalVideoPosterStore provides posters,
                    ) {
                        when (attachment?.viewer) {
                            AttachmentViewer.IMAGE -> {
                                ImageViewer(
                                    image = attachment,
                                    imageLoader = mediaLoaders.imageLoader,
                                    onDownload = { mediaSave.request(attachment) },
                                    onMenu = onMenu?.viewerMenu(attachment),
                                    onClose = onClose,
                                    navigation = navigation,
                                )
                            }

                            AttachmentViewer.VIDEO -> {
                                VideoViewer(
                                    video = attachment,
                                    loaders = mediaLoaders,
                                    onDownload = { mediaSave.request(attachment) },
                                    onMenu = onMenu?.viewerMenu(attachment),
                                    onClose = onClose,
                                    navigation = navigation,
                                )
                            }

                            AttachmentViewer.NONE, null -> {
                                Unit
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun MediaViewerWindow(
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            // Просмотрщик сам анимирует фон и закрытие: окно не затемняет и не анимирует страницу под собой.
            window?.setDimAmount(0f)
            window?.setWindowAnimations(0)
        }
        content()
    }
}

private const val MEDIA_CONTENT_STATE_KEY = "media-underlying-content"

private fun ((MediaMenuRequest) -> Unit).viewerMenu(attachment: MediaAttachmentUi): (MediaViewerMenu) -> Unit =
    { menu -> this(MediaMenuRequest(attachment, MediaMenuPlace.VIEWER, menu)) }
