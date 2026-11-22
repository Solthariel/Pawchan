package com.solthariel.pawchan.downloads

import androidx.compose.runtime.Composable
import com.solthariel.pawchan.core.ui.ActionMenuItem
import com.solthariel.pawchan.core.ui.AppMenuDialog
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.navigation.AppRoute

internal data class DownloadActionHandlers(
    val close: () -> Unit,
    val open: () -> Unit,
    val thread: () -> Unit,
    val archive: () -> Unit,
    val copy: () -> Unit,
    val retry: () -> Unit,
)

@Composable
internal fun DownloadActions(
    record: DownloadRecord,
    actions: DownloadActionHandlers,
) {
    AppMenuDialog(actions.close) {
        if (record.status == DownloadStatus.SUCCEEDED) {
            ActionMenuItem("Открыть файл", actions.open, icon = UiIconKind.DOCUMENT)
        }
        if (record.origin.threadRoute?.let(AppRoute::threadAddress) != null) {
            ActionMenuItem(
                "Перейти к посту",
                actions.thread,
                icon = UiIconKind.FORUM,
            )
        }
        if (record.origin.archiveId != null) ActionMenuItem("Перейти к посту", actions.archive, icon = UiIconKind.FORUM)
        ActionMenuItem("Копировать ссылку", actions.copy, icon = UiIconKind.LINK)
        if (record.status == DownloadStatus.FAILED || record.status == DownloadStatus.CANCELLED) {
            ActionMenuItem("Повторить", actions.retry, icon = UiIconKind.REFRESH)
        }
    }
}
