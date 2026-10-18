package com.solthariel.pawchan.media

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/** The caller keeps this session while its gallery window is temporarily hidden by a viewer. */
class MediaGallerySession internal constructor(
    internal val list: LazyListState = LazyListState(),
) {
    internal var filter by mutableStateOf(GalleryFilter.ALL)
    internal var selecting by mutableStateOf(false)
    internal var selected by mutableStateOf(emptySet<String>())

    internal fun toggleSelection() {
        selecting = !selecting
        selected = emptySet()
    }

    /** Выход из галереи завершает выделение; просмотрщик поверх галереи его не трогает. */
    fun clearSelection() {
        selecting = false
        selected = emptySet()
    }

    /** Кружок «Все»: выбирает всё видимое или снимает выбор, если всё уже выбрано. */
    internal fun selectAll(keys: List<String>) {
        selected = if (selected.containsAll(keys)) selected - keys.toSet() else selected + keys
    }

    internal fun toggle(id: String) {
        selected = if (id in selected) selected - id else selected + id
    }
}

@Composable
fun rememberMediaGallerySession(): MediaGallerySession =
    rememberSaveable(saver = GallerySessionSaver) {
        MediaGallerySession()
    }

internal val GallerySessionSaver =
    mapSaver(
        save = { session: MediaGallerySession ->
            mapOf(
                "filter" to session.filter.name,
                "selecting" to session.selecting,
                "selected" to session.selected.toList(),
                "index" to session.list.firstVisibleItemIndex,
                "offset" to session.list.firstVisibleItemScrollOffset,
            )
        },
        restore = { values ->
            MediaGallerySession(
                LazyListState(
                    (values["index"] as? Int ?: 0).coerceAtLeast(0),
                    (values["offset"] as? Int ?: 0).coerceAtLeast(0),
                ),
            ).apply {
                filter = GalleryFilter.entries.firstOrNull { it.name == values["filter"] } ?: GalleryFilter.ALL
                selecting = values["selecting"] as? Boolean ?: false
                selected = (values["selected"] as? List<*>)?.filterIsInstance<String>()?.toSet().orEmpty()
            }
        },
    )

/** Действия экрана над галереей; null — пункт недоступен. */
data class MediaGalleryActions(
    val onClose: () -> Unit,
    val onOpen: (MediaAttachmentUi) -> Unit,
    val onMenu: ((MediaMenuRequest) -> Unit)? = null,
    val onDownloadAll: ((List<MediaAttachmentUi>) -> Unit)? = null,
    val onFavoriteAll: ((List<MediaAttachmentUi>) -> Unit)? = null,
)
