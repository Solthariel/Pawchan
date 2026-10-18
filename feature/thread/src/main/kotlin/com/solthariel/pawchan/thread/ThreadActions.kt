package com.solthariel.pawchan.thread

import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.thread.watch.ThreadWatcherUiState

internal data class ThreadActions(
    val onBack: () -> Unit,
    val onRefresh: () -> Unit,
    val onFavoriteToggle: () -> Unit,
    val onChallenge: (String) -> Unit,
    val onWatcherToggle: () -> Unit = {},
    /** Открыть ответ; текст (цитата или >>номер) вставляется в форму, null — пустая форма. */
    val onReplyTo: (String?) -> Unit = {},
    val history: ThreadHistoryActions? = null,
)

/** Пункты «Содержимое ›» меню треда, работающие с историей (как в Dashchan). */
internal data class ThreadHistoryActions(
    val clearOld: () -> Unit,
    val clearDeleted: () -> Unit,
    val erase: () -> Unit,
    /** Добавляет посты копии (Архивача) удалёнными; результат — число восстановленных постов. */
    val restore: (ThreadSnapshot, (Int) -> Unit) -> Unit = { _, _ -> },
)

internal data class ThreadPresentation(
    val watcherState: ThreadWatcherUiState = ThreadWatcherUiState(),
    val reports: ThreadReportController? = null,
    val postContext: ThreadPostContext? = null,
)
