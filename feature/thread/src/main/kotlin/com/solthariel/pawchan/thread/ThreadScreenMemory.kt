package com.solthariel.pawchan.thread

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.solthariel.pawchan.media.MediaAttachmentUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class ThreadScreenMemory(
    val list: LazyListState,
    val tools: ThreadToolsState,
    private val scope: CoroutineScope,
    private val bodySize: MutableIntState,
    private val maxLines: MutableIntState,
    val gallery: com.solthariel.pawchan.media.MediaGallerySession,
) {
    private var searchScroll: Job? = null
    private var searchTarget: Long? = null

    fun moveSearch(
        content: ThreadUiState.Content?,
        delta: Int,
    ) {
        val positions = content?.presentation?.postPositions ?: return
        val anchor = if (searchScroll?.isActive == true) searchTarget?.let(positions::get) else null
        val target = threadSearchTarget(tools.results, positions, anchor ?: list.firstVisibleItemIndex, delta) ?: return
        tools.resultIndex = tools.results.indexOf(target)
        searchTarget = target
        searchScroll?.cancel()
        searchScroll = scope.launch { list.animateScrollToItem(positions.getValue(target)) }
    }

    var opened by mutableStateOf<MediaAttachmentUi?>(null)
    var syncThreadPosition: Boolean = false

    fun closeViewer(content: ThreadUiState.Content?) {
        if (syncThreadPosition) opened?.source?.ownerPostId?.let { goTo(content, it) }
        opened = null
    }

    var mediaMenu by mutableStateOf<com.solthariel.pawchan.media.MediaMenuRequest?>(null)
    var onShowHidden: (ThreadPostUi) -> Unit = {}
    val appearance: ThreadAppearance get() = ThreadAppearance(bodySize.intValue, maxLines.intValue)

    fun updateAppearance(value: ThreadAppearance) {
        bodySize.intValue = value.textSize
        maxLines.intValue = value.maxLines
    }

    /** «Перейти к посту» из меню файла: закрыть просмотрщик, галерею и превью, прокрутить к посту. */
    fun goToPost(
        content: ThreadUiState.Content?,
        attachment: MediaAttachmentUi,
    ) {
        opened = null
        if (tools.dialog == ThreadDialog.GALLERY) {
            gallery.clearSelection()
            tools.dialog = ThreadDialog.NONE
        }
        tools.previews = emptyList()
        attachment.source.ownerPostId?.let { goTo(content, it) }
    }

    fun goTo(
        content: ThreadUiState.Content?,
        id: Long,
    ) {
        content
            ?.presentation
            ?.postPositions
            ?.get(id)
            ?.let { index -> scope.launch { list.animateScrollToItem(index) } }
    }
}

@Composable
internal fun rememberThreadScreenMemory(
    content: ThreadUiState.Content?,
    address: com.solthariel.pawchan.domain.model.ThreadAddress?,
    reading: ThreadReadingSession?,
): ThreadScreenMemory {
    val scope = rememberCoroutineScope()
    val gallery =
        androidx.compose.runtime.key(address) {
            com.solthariel.pawchan.media
                .rememberMediaGallerySession()
        }
    val anchor =
        remember(address, content != null, reading) {
            threadListAnchor(content?.presentation?.postPositions, address?.let { reading?.restore(it) })
        }
    val list =
        androidx.compose.runtime.key(address, content != null, reading) {
            rememberLazyListState(anchor.first, anchor.second)
        }
    // Значения из глобальных настроек — только начальные: в треде их меняют через «Вид».
    val defaults = com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences.current
    val body = rememberSaveable { mutableIntStateOf(defaults.textSize) }
    val maxLines = rememberSaveable { mutableIntStateOf(defaults.collapseLines) }
    val syncPosition = com.solthariel.pawchan.media.LocalMediaPreferences.current.syncThreadPosition
    val memory =
        remember(
            list,
            scope,
            body,
            maxLines,
            gallery,
        ) { ThreadScreenMemory(list, ThreadToolsState(scope), scope, body, maxLines, gallery) }
    androidx.compose.runtime.SideEffect { memory.syncThreadPosition = syncPosition }
    return memory
}
