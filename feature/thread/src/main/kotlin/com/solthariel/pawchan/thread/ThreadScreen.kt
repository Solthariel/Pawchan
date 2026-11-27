package com.solthariel.pawchan.thread

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.MediaContentHost
import com.solthariel.pawchan.media.MediaGalleryContent
import com.solthariel.pawchan.media.MediaSaveActions
import com.solthariel.pawchan.media.MediaViewerSelection
import com.solthariel.pawchan.media.rememberMediaSaveActions
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader
import com.solthariel.pawchan.thread.vocaroo.LocalVocarooPlayer
import com.solthariel.pawchan.thread.vocaroo.rememberVocarooPlayer

@Composable
internal fun ThreadScreen(
    state: ThreadUiState,
    actions: ThreadActions,
    presentation: ThreadPresentation,
    host: ThreadScreenHost,
    requestPost: suspend (Long) -> Unit = {},
    contentHost: @Composable (content: @Composable () -> Unit) -> Unit = { content -> content() },
) {
    val media = host.mediaHost.resources
    val content = state as? ThreadUiState.Content
    val memory = rememberThreadScreenMemory(content, presentation.postContext?.address, host.reading)
    val hideStickers = LocalPostDisplayPreferences.current.hideStickers
    val save = rememberMediaSaveActions(media.saves)
    val exports = rememberThreadExportActions(host.exportTarget, memory.tools)
    val selection = rememberThreadSelectionActions(content, memory.tools, actions, presentation, exports)
    memory.onShowHidden = selection.showHidden
    val refreshActions = actions.withThumbnailRetry(content, media.thumbnails)
    val vocaroo = rememberVocarooPlayer(media.vocaroo, media.viewer.contentClient)
    val spoilers = remember { RevealedSpoilers() }
    BindThreadScreenEffects(memory, content, actions, exports)
    BindThreadPendingPost(memory, content, presentation.postContext?.address, host.reading, requestPost)
    BindThreadReadingPosition(memory, content, presentation.postContext?.address, host.reading)
    BindThreadReadingReturn(memory, content, presentation.postContext?.address, host.reading)
    contentHost {
        BindVisibleReplies(memory, content, content?.address ?: presentation.postContext?.address, host.reading)
        MediaContentHost(
            MediaViewerSelection(
                memory.opened,
                memory.viewerItems(content, hideStickers),
            ) {
                memory.opened = it
            },
            media.viewer,
            save,
            { memory.mediaMenu = it },
            { memory.closeViewer(content) },
        ) {
            CompositionLocalProvider(
                LocalAddContentFilter provides host.contentFilterTools?.add,
                LocalEditContentFilter provides host.contentFilterTools?.edit,
                LocalThreadUnreadIndicator provides host.unread,
                LocalThreadArchive provides host.archive,
                LocalVocarooPlayer provides vocaroo,
                LocalRevealedSpoilers provides spoilers,
                LocalPostLinkAddress provides (content?.address ?: presentation.postContext?.address),
                LocalPostLinkMedia provides { memory.opened = it },
                LocalPostPreview provides { reference ->
                    memory.tools.openPostPreview(
                        PostPreviewTarget(reference.thread ?: presentation.postContext?.address, reference.post.value),
                        content,
                    )
                },
            ) {
                val bindings = memory.contentBindings(media.thumbnails, save, refreshActions, selection)
                val refreshFailed = presentation.watcherState.error != null
                ThreadWorkspace(state, refreshActions, bindings, memory, selection, refreshFailed)
                // Окна-источники (галерея, превью) создаются раньше окна просмотрщика и остаются под ним,
                // в том числе после пересоздания активности.
                content?.let { thread ->
                    ThreadSourceOverlays(thread, save, memory, refreshActions, presentation, host)
                }
            }
        }
    }
    host.contentFilterTools?.editor?.invoke(content?.presentation?.filterSamples.orEmpty())
    ThreadScreenMediaMenu(content, host.mediaHost, memory, save)
}

private fun ThreadActions.withThumbnailRetry(
    content: ThreadUiState.Content?,
    thumbnails: ThumbnailLoader,
): ThreadActions =
    copy(onRefresh = {
        if ((content?.presentation?.missingThreadNotice ?: 0) == 0L) thumbnails.retryVisible()
        onRefresh()
    })

@Composable
private fun ThreadScreenMediaMenu(
    content: ThreadUiState.Content?,
    host: ThreadMediaHost,
    memory: ThreadScreenMemory,
    save: MediaSaveActions,
) {
    host.menu(
        ThreadMediaMenu(
            memory.mediaMenu,
            save,
            { memory.tools.notice = it },
            { memory.goToPost(content, it) },
            { memory.mediaMenu = null },
        ),
    )
}

@Composable
private fun ThreadSourceOverlays(
    thread: ThreadUiState.Content,
    save: MediaSaveActions,
    memory: ThreadScreenMemory,
    actions: ThreadActions,
    presentation: ThreadPresentation,
    host: ThreadScreenHost,
) {
    val media = host.mediaHost.resources
    ThreadPostMenuHost(thread, memory.tools, actions, presentation)
    ThreadScreenDialogs(
        thread,
        memory.tools,
        ThreadDialogBindings(
            memory.appearance,
            memory::updateAppearance,
            presentation.reports,
            media.thumbnails,
            host.archive,
            actions.history,
            presentation.postContext?.preferences,
        ),
        ThreadGalleryBindings(memory.gallery, { memory.mediaMenu = it }, save.requestAll, media.favorite),
    ) { memory.openFromGallery(it) }
    ThreadPreviewHost(thread, memory, save, presentation, host.preview)
}

private fun ThreadUiState.Content?.viewerItems(): List<MediaAttachmentUi> =
    this
        ?.presentation
        ?.gallery
        ?.all
        ?.items
        .orEmpty()

private fun ThreadScreenMemory.viewerItems(
    content: ThreadUiState.Content?,
    hideStickers: Boolean,
): List<MediaAttachmentUi> =
    if (fromGallery) {
        gallery.viewerItems(content?.presentation?.gallery ?: MediaGalleryContent(), hideStickers)
    } else {
        content.viewerItems()
    }
