package com.solthariel.pawchan.downloads

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.solthariel.pawchan.PawchanApplication
import com.solthariel.pawchan.catalog.CatalogIconButton
import com.solthariel.pawchan.core.ui.ActionMenuItem
import com.solthariel.pawchan.core.ui.AppAlertDialog
import com.solthariel.pawchan.core.ui.FloatingTopChrome
import com.solthariel.pawchan.core.ui.LocalBottomOverlayInset
import com.solthariel.pawchan.core.ui.LocalTopOverlayInset
import com.solthariel.pawchan.core.ui.ModalDropdownMenu
import com.solthariel.pawchan.core.ui.TransientNotice
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.infrastructure.media.MediaDownloadQueue
import com.solthariel.pawchan.library.LibraryEmpty
import com.solthariel.pawchan.library.LibraryToolbar
import com.solthariel.pawchan.library.LibraryToolbarActions
import com.solthariel.pawchan.library.LibraryToolbarState
import com.solthariel.pawchan.navigation.AppRoute
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.navigation.chrome.AppBottomDestination
import com.solthariel.pawchan.navigation.chrome.AppNavigationWorkspace
import com.solthariel.pawchan.ui.copyPublicLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun DownloadsScreen(
    queue: MediaDownloadQueue,
    onBack: () -> Unit,
    onThread: (ThreadAddress, Long?) -> Unit,
    onArchive: (Long) -> Unit,
    bottomActions: AppBottomActions,
    owner: ViewModelStoreOwner,
) {
    val context = LocalContext.current
    val store = (context.applicationContext as PawchanApplication).downloadHistory
    val history by store.cachedRecords.collectAsStateWithLifecycle()
    val records = (history as? DownloadHistoryState.Content)?.records.orEmpty()
    LaunchedEffect(store) { store.refresh() }
    val workManager = remember { WorkManager.getInstance(context.applicationContext) }
    val works by remember(workManager) { workManager.getWorkInfosByTagFlow("neko-media") }
        .collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val thumbnails = viewModel<DownloadsCacheViewModel>(viewModelStoreOwner = owner).thumbnails
    var query by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<DownloadRecord?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    DownloadRefreshNotice(history) { notice = it }
    val prepared by produceState<PreparedDownloads?>(null, records, works, query) {
        value = withContext(Dispatchers.Default) { prepareDownloads(records, works, query) }
    }
    val resolved = prepared?.resolved.orEmpty()
    ReconcileDownloads(store, prepared?.source.orEmpty(), resolved)
    val visible = prepared?.visible
    AppNavigationWorkspace(actions = bottomActions, selected = AppBottomDestination.THREADS, floatingTop = true) {
        FloatingTopChrome(toolbar = {
            LibraryToolbar(
                LibraryToolbarState("Загрузки", visible?.size, query, false),
                LibraryToolbarActions(
                    onBack,
                    { query = it },
                    extraMenu = {
                        DownloadsMenu(resolved.any { it.status in FINISHED_STATUSES }) { confirmClear = true }
                    },
                ),
            )
        }) {
            DownloadList(
                visible = if (history is DownloadHistoryState.Content) visible else null,
                loadError = history == DownloadHistoryState.Error,
                thumbnails = thumbnails,
                onSelect = { selected = it },
                onOpen = { record -> openDownloadedFile(context, record, scope) { notice = it } },
            )
        }
    }
    selected?.let { item ->
        DownloadSelection(item, resolved, queue, DownloadNavigation(onThread, onArchive), { selected = null }) {
            notice = it
        }
    }
    TransientNotice(
        notice,
        eventKey = (history as? DownloadHistoryState.Content)?.refreshFailures to notice,
        onDismiss = { notice = null },
    )
    if (confirmClear) {
        DownloadClearDialog({ confirmClear = false }) {
            scope.launch(Dispatchers.IO) { store.clearFinished() }
        }
    }
}

@Composable
private fun DownloadRefreshNotice(
    history: DownloadHistoryState,
    showNotice: (String) -> Unit,
) {
    LaunchedEffect(history) {
        if (((history as? DownloadHistoryState.Content)?.refreshFailures ?: 0) > 0) {
            showNotice("Не удалось обновить загрузки")
        }
    }
}

@Composable
private fun DownloadSelection(
    item: DownloadRecord,
    resolved: List<DownloadRecord>,
    queue: MediaDownloadQueue,
    navigation: DownloadNavigation,
    close: () -> Unit,
    showNotice: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val record = resolved.firstOrNull { it.id == item.id } ?: item
    DownloadSelectedActions(record, queue, close, navigation, scope, showNotice)
}

@Composable
internal fun DownloadList(
    visible: List<DownloadRecord>?,
    thumbnails: LruCache<String, Bitmap>,
    onSelect: (DownloadRecord) -> Unit,
    onOpen: (DownloadRecord) -> Unit,
    loadError: Boolean = false,
) {
    if (visible == null || visible.isEmpty()) {
        val message =
            when {
                loadError -> "Ошибка при загрузке данных"
                visible == null -> "Загрузка…"
                else -> "Загрузок нет"
            }
        Box(Modifier.padding(start = 8.dp, top = LocalTopOverlayInset.current)) { LibraryEmpty(message) }
    } else {
        LazyColumn(
            contentPadding =
                PaddingValues(
                    top = LocalTopOverlayInset.current,
                    bottom = LocalBottomOverlayInset.current + 8.dp,
                ),
        ) {
            items(visible, key = { it.id }, contentType = { "download-row" }) { record ->
                DownloadRow(record, thumbnails, { onSelect(record) }, { onOpen(record) })
            }
        }
    }
}

@Composable
private fun ReconcileDownloads(
    store: DownloadHistoryStore,
    records: List<DownloadRecord>,
    resolved: List<DownloadRecord>,
) {
    LaunchedEffect(resolved) {
        withContext(Dispatchers.IO) {
            resolved.zip(records).forEach { (current, old) ->
                if (current.status != old.status &&
                    current.status in setOf(DownloadStatus.SUCCEEDED, DownloadStatus.FAILED, DownloadStatus.CANCELLED)
                ) {
                    store.setStatusIfUnchanged(current.id, old.status, current.status)
                }
            }
        }
    }
}

private data class DownloadNavigation(
    val onThread: (ThreadAddress, Long?) -> Unit,
    val onArchive: (Long) -> Unit,
)

@Composable
private fun DownloadSelectedActions(
    record: DownloadRecord,
    queue: MediaDownloadQueue,
    close: () -> Unit,
    navigation: DownloadNavigation,
    scope: CoroutineScope,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    DownloadActions(
        record,
        DownloadActionHandlers(
            close = close,
            open = {
                close()
                openDownloadedFile(context, record, scope, onError)
            },
            thread = {
                close()
                record.origin.threadRoute
                    ?.let(AppRoute::threadAddress)
                    ?.let { navigation.onThread(it, record.origin.postId) }
            },
            archive = {
                close()
                record.origin.archiveId?.let(navigation.onArchive)
            },
            copy = {
                close()
                context.copyPublicLink(record.url)
            },
            retry = {
                close()
                scope.launch {
                    val success = withContext(Dispatchers.IO) { queue.retry(record) }
                    if (!success) onError("Не удалось повторить загрузку")
                }
            },
        ),
    )
}

@Composable
private fun DownloadClearDialog(
    close: () -> Unit,
    clear: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = close,
        title = { Text("Очистить историю загрузок?") },
        text = { Text("Завершённые записи будут удалены из истории. Сами файлы останутся на устройстве.") },
        confirmButton = {
            TextButton(onClick = {
                close()
                clear()
            }) { Text("Очистить") }
        },
        dismissButton = { TextButton(onClick = close) { Text("Отмена") } },
    )
}

internal fun resolvedStatus(
    record: DownloadRecord,
    state: WorkInfo.State?,
    loaded: Boolean,
): DownloadStatus {
    val terminal =
        when (state) {
            WorkInfo.State.SUCCEEDED -> DownloadStatus.SUCCEEDED
            WorkInfo.State.FAILED -> DownloadStatus.FAILED
            WorkInfo.State.CANCELLED -> DownloadStatus.CANCELLED
            else -> null
        }
    return terminal ?: when {
        record.status in setOf(DownloadStatus.SUCCEEDED, DownloadStatus.FAILED, DownloadStatus.CANCELLED) -> {
            record.status
        }

        state == WorkInfo.State.RUNNING -> {
            DownloadStatus.RUNNING
        }

        state == WorkInfo.State.ENQUEUED || state == WorkInfo.State.BLOCKED -> {
            if (record.status == DownloadStatus.WAITING) record.status else DownloadStatus.QUEUED
        }

        state == null && loaded && System.currentTimeMillis() - record.created > ORPHAN_GRACE_MILLIS -> {
            DownloadStatus.FAILED
        }

        else -> {
            record.status
        }
    }
}

@Composable
private fun DownloadRow(
    record: DownloadRecord,
    thumbnails: LruCache<String, Bitmap>,
    onClick: () -> Unit,
    onOpen: () -> Unit,
) {
    val source =
        when {
            record.origin.archiveId != null -> {
                "Архивач"
            }

            record.origin.threadRoute != null -> {
                when (val site = AppRoute.threadAddress(record.origin.threadRoute)?.site?.value) {
                    "dvach" -> "2ch"
                    null -> "Источник неизвестен"
                    else -> site
                }
            }

            else -> {
                "Источник неизвестен"
            }
        }
    val status =
        when (record.status) {
            DownloadStatus.QUEUED -> "В очереди"
            DownloadStatus.RUNNING -> "Загрузка"
            DownloadStatus.WAITING -> "Ожидание повторной попытки"
            DownloadStatus.SUCCEEDED -> "Сохранено"
            DownloadStatus.FAILED -> "Ошибка загрузки"
            DownloadStatus.CANCELLED -> "Отменено"
        }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DownloadThumbnail(record, thumbnails, onOpen)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                record.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "$source · $status",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    HorizontalDivider()
}

@Composable
private fun DownloadsMenu(
    canClear: Boolean,
    onClear: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        CatalogIconButton(UiIconKind.MORE, "Меню") { open = true }
        ModalDropdownMenu(open, { open = false }) {
            ActionMenuItem("Очистить историю", {
                open = false
                onClear()
            }, enabled = canClear)
        }
    }
}

private val FINISHED_STATUSES = setOf(DownloadStatus.SUCCEEDED, DownloadStatus.FAILED, DownloadStatus.CANCELLED)

private const val ORPHAN_GRACE_MILLIS = 60_000L

private fun openDownloadedFile(
    context: Context,
    record: DownloadRecord,
    scope: CoroutineScope,
    onError: (String) -> Unit,
) {
    scope.launch {
        val uri = record.destination.toUri()
        try {
            val type = withContext(Dispatchers.IO) { context.contentResolver.getType(uri) ?: "*/*" }
            context.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, type)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
            )
        } catch (_: ActivityNotFoundException) {
            onError("Не удалось открыть файл")
        } catch (_: SecurityException) {
            onError("Файл недоступен")
        } catch (_: IllegalArgumentException) {
            onError("Файл недоступен")
        }
    }
}
