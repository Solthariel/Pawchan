package com.solthariel.pawchan.thread

import android.net.Uri
import androidx.compose.runtime.Composable
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.MediaMenuRequest
import com.solthariel.pawchan.media.MediaSaveActions
import com.solthariel.pawchan.media.MediaSaveQueue
import com.solthariel.pawchan.media.MediaViewerResources
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader

/** Media resources are borrowed; the screen never creates or closes app loaders. */
data class ThreadMediaResources(
    val thumbnails: ThumbnailLoader,
    val viewer: MediaViewerResources,
    val saves: MediaSaveQueue,
    /** Добавить выбранные в галерее файлы в избранное; null — избранного нет. */
    val favorite: ((List<MediaAttachmentUi>) -> Unit)? = null,
    /** Сеть для записей Vocaroo (DoH, Cronet); null — обычный клиент медиа. */
    val vocaroo: com.solthariel.pawchan.thread.vocaroo.VocarooTransport? = null,
)

/** Меню файла, открытое на экране треда; [goToPost] закрывает окна медиа и прокручивает к посту. */
data class ThreadMediaMenu(
    val request: MediaMenuRequest?,
    val save: MediaSaveActions,
    val notice: (String) -> Unit,
    val goToPost: (MediaAttachmentUi) -> Unit,
    val close: () -> Unit,
)

/** App-owned capabilities. Slots never receive mutable thread tools or screen memory. */
data class ThreadScreenHost(
    val mediaHost: ThreadMediaHost,
    val reading: ThreadReadingSession?,
    val archive: ThreadArchiveHost,
    val preview: PostPreviewResources?,
    val exportTarget: (Uri) -> ThreadExportTarget,
    val contentFilterTools: ThreadContentFilterTools? = null,
    val unread: @Composable () -> Boolean = { false },
)

/** App supplies both borrowed media resources and the menu operating on those resources. */
data class ThreadMediaHost(
    val resources: ThreadMediaResources,
    val menu: @Composable (ThreadMediaMenu) -> Unit,
)
