package com.solthariel.pawchan.threads

internal data class ThreadRowCloseAction(
    val label: String,
    val onClick: () -> Unit,
)

/** These remove local list entries, never remote threads or posts. */
internal fun threadRowCloseAction(
    section: ThreadsSection,
    row: ThreadRowUi,
    actions: ThreadsScreenActions,
): ThreadRowCloseAction =
    when (section) {
        ThreadsSection.PAGES -> {
            ThreadRowCloseAction("Закрыть страницу") { actions.pages.close(row.address) }
        }

        ThreadsSection.FAVORITES -> {
            ThreadRowCloseAction("Убрать из избранного") { actions.setFavorite(row, false) }
        }

        ThreadsSection.REPLIES -> {
            ThreadRowCloseAction("Скрыть ответы в этом треде") { actions.dismissReplies(row.address) }
        }
    }
