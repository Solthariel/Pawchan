package com.solthariel.pawchan.media

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.solthariel.pawchan.core.media.R
import com.solthariel.pawchan.core.ui.ActionMenuItem
import com.solthariel.pawchan.core.ui.AppDialog
import com.solthariel.pawchan.domain.model.MediaKind
import com.solthariel.pawchan.media.metadata.MediaMetadataDialog
import com.solthariel.pawchan.media.metadata.MediaMetadataSource
import com.solthariel.pawchan.media.metadata.MediaMetadataSummary
import com.solthariel.pawchan.media.metadata.rememberMediaMetadata

enum class ImageSearchEngine {
    GOOGLE,
    YANDEX,
}

/** Откуда открыто меню файла: набор пунктов зависит от места, как в Dashchan. */
enum class MediaMenuPlace {
    /** Миниатюра в посте: загрузка и ссылка. */
    THUMBNAIL,

    /** Плитка галереи: плюс поиск и переход к посту. */
    GALLERY,

    /** Открытый файл (⋮ или долгое нажатие): полный набор. */
    VIEWER,
}

/** Пункты, которые знает только открытый просмотрщик. */
data class MediaViewerMenu(
    /** «Обновить» дублируется в меню, только когда панели скрыты. */
    val onRefresh: (() -> Unit)? = null,
    /** null — не видео; иначе текущее состояние программного декодера. */
    val softwareDecoder: Boolean? = null,
    val onSoftwareDecoder: () -> Unit = {},
    /** null — сведения о файле недоступны. */
    val metadata: MediaMetadataSource? = null,
    /** Открыто кнопкой ⋮: меню всплывает у кнопки и показывает сводку метаданных, как в Dashchan. */
    val anchored: Boolean = false,
)

data class MediaMenuRequest(
    val attachment: MediaAttachmentUi,
    val place: MediaMenuPlace,
    val viewer: MediaViewerMenu? = null,
)

data class MediaMenuActions(
    val onDownload: () -> Unit,
    val onCopyLink: () -> Unit,
    val onCopyImage: (() -> Unit)?,
    val onSearch: (ImageSearchEngine) -> Unit,
    val onFavorite: () -> Unit,
    /** null — экран не умеет переходить к посту (каталог, сохранённое). */
    val onGoToPost: (() -> Unit)? = null,
    /** null — файл открыт не из каталога. */
    val onOpenThread: (() -> Unit)? = null,
)

/** Пункты меню в порядке Dashchan для данного места. */
internal enum class MediaMenuItem(
    @param:StringRes val label: Int,
) {
    METADATA(R.string.media_menu_metadata),
    OPEN_THREAD(R.string.media_menu_open_thread),
    REFRESH(R.string.media_menu_refresh),
    DOWNLOAD(R.string.media_menu_download),
    SEARCH(R.string.media_menu_image_search),
    COPY_IMAGE(R.string.media_menu_copy_image),
    GO_TO_POST(R.string.media_menu_go_to_post),
    COPY_LINK(R.string.media_menu_copy_link),
    FAVORITE(R.string.media_menu_favorite),
    SOFTWARE_DECODER(R.string.media_menu_software_decoder),
}

/**
 * Сверху то, что нужно в этом месте, снизу общий блок «Скачать файл · Копировать ссылку».
 * Превью в посте и в каталоге одинаковы; галерее нужен переход к посту, просмотрщику — всё, что требует
 * открытого файла.
 */
internal fun mediaMenuItems(
    request: MediaMenuRequest,
    canCopyImage: Boolean,
    canGoToPost: Boolean,
    canOpenThread: Boolean = false,
): List<MediaMenuItem> {
    val attachment = request.attachment
    val search = MediaMenuItem.SEARCH.takeIf { attachment.kind == MediaKind.IMAGE || attachment.thumbnailUrl != null }
    val viewer = request.viewer
    val specific =
        when (request.place) {
            MediaMenuPlace.THUMBNAIL -> {
                listOfNotNull(search)
            }

            MediaMenuPlace.GALLERY -> {
                listOfNotNull(MediaMenuItem.GO_TO_POST.takeIf { canGoToPost }, search)
            }

            MediaMenuPlace.VIEWER -> {
                listOfNotNull(
                    MediaMenuItem.METADATA.takeIf { viewer?.metadata != null },
                    search,
                    MediaMenuItem.COPY_IMAGE.takeIf { canCopyImage },
                    MediaMenuItem.GO_TO_POST.takeIf { canGoToPost },
                    MediaMenuItem.OPEN_THREAD.takeIf { canOpenThread },
                    MediaMenuItem.REFRESH.takeIf { viewer?.onRefresh != null },
                )
            }
        }
    val decoder = MediaMenuItem.SOFTWARE_DECODER.takeIf { viewer?.softwareDecoder != null }
    return specific + COMMON_MENU_ITEMS + listOfNotNull(decoder)
}

private val COMMON_MENU_ITEMS = listOf(MediaMenuItem.DOWNLOAD, MediaMenuItem.COPY_LINK)

@Composable
fun MediaContextMenu(
    request: MediaMenuRequest,
    actions: MediaMenuActions,
    onClose: () -> Unit,
) {
    val metadataSource = request.viewer?.metadata
    val metadata = rememberMediaMetadata(metadataSource)
    var details by remember(request) { mutableStateOf(false) }
    when {
        details -> {
            MediaMetadataDialog(metadata, onClose)
        }

        request.viewer?.anchored == true -> {
            AnchoredMediaMenu(onClose) {
                MediaMenuBody(request, actions, onClose) { details = true }
                if (metadataSource != null) MediaMetadataSummary(metadata) { details = true }
            }
        }

        else -> {
            CenteredMediaMenu(onClose) {
                Text(
                    request.attachment.fileName,
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                MediaMenuBody(request, actions, onClose) { details = true }
            }
        }
    }
}

@Composable
private fun CenteredMediaMenu(
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    AppDialog(onClose) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(8.dp), content = content)
    }
}

/** Всплывающее меню у ⋮ в правом верхнем углу, без затемнения — как overflow в Dashchan. */
@Composable
private fun AnchoredMediaMenu(
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0f) }
        Box(Modifier.fillMaxSize().clickable(interactionSource = null, indication = null, onClick = onClose)) {
            Surface(
                Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 4.dp, end = 4.dp)
                    .widthIn(min = 220.dp, max = 300.dp),
                shape = RoundedCornerShape(8.dp),
                shadowElevation = 8.dp,
            ) {
                Column(
                    Modifier.width(IntrinsicSize.Max).verticalScroll(rememberScrollState()).padding(vertical = 4.dp),
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun MediaMenuBody(
    request: MediaMenuRequest,
    actions: MediaMenuActions,
    onClose: () -> Unit,
    onMetadata: () -> Unit,
) {
    var search by remember(request) { mutableStateOf(false) }
    if (search) {
        MediaSearchItems(actions) { search = false }
    } else {
        val items =
            mediaMenuItems(
                request,
                actions.onCopyImage != null,
                actions.onGoToPost != null,
                actions.onOpenThread != null,
            )
        val handlers = MediaMenuHandlers(onClose, onSearch = { search = true }, onMetadata = onMetadata)
        items.forEach { item -> MediaMenuEntry(item, request, actions, handlers) }
    }
}

private class MediaMenuHandlers(
    val onClose: () -> Unit,
    val onSearch: () -> Unit,
    val onMetadata: () -> Unit,
) {
    fun closing(action: () -> Unit): () -> Unit =
        {
            onClose()
            action()
        }
}

@Composable
private fun MediaSearchItems(
    actions: MediaMenuActions,
    onBack: () -> Unit,
) {
    ActionMenuItem("‹ Назад", onBack)
    ActionMenuItem(stringResource(R.string.media_menu_google), { actions.onSearch(ImageSearchEngine.GOOGLE) })
    ActionMenuItem(stringResource(R.string.media_menu_yandex), { actions.onSearch(ImageSearchEngine.YANDEX) })
}

@Composable
private fun MediaMenuEntry(
    item: MediaMenuItem,
    request: MediaMenuRequest,
    actions: MediaMenuActions,
    handlers: MediaMenuHandlers,
) {
    val viewer = request.viewer
    val label = stringResource(item.label)
    if (item == MediaMenuItem.SOFTWARE_DECODER) {
        MediaCheckItem(label, viewer?.softwareDecoder == true, handlers.closing { viewer?.onSoftwareDecoder?.invoke() })
    } else {
        ActionMenuItem(label, mediaMenuAction(item, request, actions, handlers))
    }
}

private fun mediaMenuAction(
    item: MediaMenuItem,
    request: MediaMenuRequest,
    actions: MediaMenuActions,
    handlers: MediaMenuHandlers,
): () -> Unit =
    when (item) {
        MediaMenuItem.METADATA -> handlers.onMetadata
        MediaMenuItem.OPEN_THREAD -> handlers.closing { actions.onOpenThread?.invoke() }
        MediaMenuItem.REFRESH -> handlers.closing { request.viewer?.onRefresh?.invoke() }
        MediaMenuItem.DOWNLOAD -> actions.onDownload
        MediaMenuItem.SEARCH -> handlers.onSearch
        MediaMenuItem.COPY_IMAGE -> ({ actions.onCopyImage?.invoke() })
        MediaMenuItem.GO_TO_POST -> handlers.closing { actions.onGoToPost?.invoke() }
        MediaMenuItem.COPY_LINK -> actions.onCopyLink
        MediaMenuItem.FAVORITE -> handlers.closing(actions.onFavorite)
        MediaMenuItem.SOFTWARE_DECODER -> handlers.closing { request.viewer?.onSoftwareDecoder?.invoke() }
    }

@Composable
private fun MediaCheckItem(
    label: String,
    checked: Boolean,
    onClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { ActionMenuItem(label, onClick) }
        Checkbox(checked = checked, onCheckedChange = { onClick() })
    }
}
