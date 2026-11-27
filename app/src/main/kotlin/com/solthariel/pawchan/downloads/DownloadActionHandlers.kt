package com.solthariel.pawchan.downloads

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import com.solthariel.pawchan.core.ui.ActionMenuItem
import com.solthariel.pawchan.core.ui.AppMenuDialog
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.navigation.AppRoute
import com.solthariel.pawchan.R.string as L

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
    val res = LocalResources.current

    AppMenuDialog(actions.close) {
        if (record.status == DownloadStatus.SUCCEEDED) {
            ActionMenuItem(res.getString(L.ui_open_file), actions.open, icon = UiIconKind.DOCUMENT)
        }
        if (record.origin.threadRoute?.let(AppRoute::threadAddress) != null) {
            ActionMenuItem(
                res.getString(L.ui_go_to_post),
                actions.thread,
                icon = UiIconKind.FORUM,
            )
        }
        if (record.origin.archiveId !=
            null
        ) {
            ActionMenuItem(res.getString(L.ui_go_to_post), actions.archive, icon = UiIconKind.FORUM)
        }
        ActionMenuItem(res.getString(L.ui_copy_link), actions.copy, icon = UiIconKind.LINK)
        if (record.status == DownloadStatus.FAILED || record.status == DownloadStatus.CANCELLED) {
            ActionMenuItem(res.getString(L.ui_retry), actions.retry, icon = UiIconKind.REFRESH)
        }
    }
}
