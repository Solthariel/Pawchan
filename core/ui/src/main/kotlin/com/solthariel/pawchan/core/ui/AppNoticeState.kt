package com.solthariel.pawchan.core.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Latest event wins: a window never queues or stacks transient messages. */
internal class AppNoticeState {
    var current by mutableStateOf<AppNotice?>(null)
        private set

    fun show(notice: AppNotice) {
        current = notice
    }

    fun dismiss(notice: AppNotice) {
        if (current === notice) {
            current = null
            notice.onDismiss()
        }
    }
}

class NoticeAction(
    val label: String,
    val onClick: () -> Unit,
)

internal class AppNotice(
    val message: String,
    val action: NoticeAction?,
    bottomInset: Dp = 0.dp,
    val compactAction: Boolean = false,
    val onDismiss: () -> Unit,
) {
    var bottomInset by mutableStateOf(bottomInset)
    var timeoutMillis: Long? = null
    var actionOnly: Boolean = false
}

internal fun noticeReadingMillis(length: Int): Long =
    (NOTICE_MIN_MILLIS + length.coerceAtLeast(0).toLong() * NOTICE_CHARACTER_MILLIS).coerceAtMost(NOTICE_MAX_MILLIS)

private const val NOTICE_MIN_MILLIS = 6_000L
private const val NOTICE_MAX_MILLIS = 90_000L
private const val NOTICE_CHARACTER_MILLIS = 45L
