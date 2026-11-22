package com.solthariel.pawchan.arhivach

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.solthariel.pawchan.infrastructure.MediaLoaders
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.MediaGallery
import com.solthariel.pawchan.media.MediaGalleryActions
import com.solthariel.pawchan.media.MediaGalleryContent
import com.solthariel.pawchan.media.MediaGallerySession
import com.solthariel.pawchan.media.MediaMenuRequest
import com.solthariel.pawchan.media.MediaSaveActions
import com.solthariel.pawchan.media.rememberMediaGallerySession

internal class ArchiveMediaState(
    val session: MediaGallerySession,
    initialRequest: Int,
) {
    var opened by mutableStateOf<MediaAttachmentUi?>(null)
    var menu by mutableStateOf<MediaMenuRequest?>(null)
    var galleryVisible by mutableStateOf(false)
    private var fromGallery by mutableStateOf(false)
    private var handledRequest = initialRequest

    fun requestGallery(
        request: Int,
        gallery: MediaGalleryContent?,
    ) {
        if (request != handledRequest && gallery != null) {
            galleryVisible = true
            handledRequest = request
        }
    }

    fun openPost(attachment: MediaAttachmentUi) {
        fromGallery = false
        opened = attachment
    }

    fun openGallery(attachment: MediaAttachmentUi) {
        fromGallery = true
        opened = attachment
    }

    fun closeViewer() {
        opened = null
        fromGallery = false
    }

    fun viewerItems(
        presentation: ArchiveThreadPresentationUi?,
        hideStickers: Boolean,
    ): List<MediaAttachmentUi> =
        if (fromGallery && presentation != null) {
            session.viewerItems(presentation.gallery, hideStickers)
        } else {
            presentation?.media.orEmpty()
        }
}

@Composable
internal fun rememberArchiveMediaState(
    threadId: Long?,
    galleryRequest: Int,
    gallery: MediaGalleryContent?,
): ArchiveMediaState {
    val session = key(threadId) { rememberMediaGallerySession() }
    val state = remember(threadId, session) { ArchiveMediaState(session, galleryRequest) }
    LaunchedEffect(galleryRequest, gallery, state) { state.requestGallery(galleryRequest, gallery) }
    return state
}

@Composable
internal fun ArchiveMediaGallery(
    gallery: MediaGalleryContent?,
    loaders: MediaLoaders,
    saves: MediaSaveActions,
    state: ArchiveMediaState,
) {
    if (gallery == null) return
    MediaGallery(
        gallery,
        loaders.thumbnailLoader,
        MediaGalleryActions(
            onClose = { state.galleryVisible = false },
            onOpen = state::openGallery,
            onMenu = { state.menu = it },
            onDownloadAll = saves.requestAll,
        ),
        state.session,
        visible = state.galleryVisible,
    )
}
