package com.solthariel.pawchan.threads

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import com.solthariel.pawchan.navigation.saveableKey

internal fun LazyListScope.threadsSectionRows(
    section: ThreadsSection,
    rows: List<ThreadRowUi>?,
    list: ThreadsListContext,
) {
    when {
        rows == null -> {
            item(key = "loading-${section.name}") { ThreadsEmptyRow("Загрузка…") }
        }

        rows.isEmpty() -> {
            item(key = "empty-${section.name}") {
                ThreadsEmptyRow(if (list.searching) "Совпадений нет" else section.emptyText())
            }
        }

        else -> {
            val limit =
                when (section) {
                    ThreadsSection.PAGES -> list.limits.pages
                    ThreadsSection.FAVORITES -> list.limits.favorites
                    ThreadsSection.REPLIES -> list.limits.replies
                }.coerceIn(
                    com.solthariel.pawchan.domain.settings.MIN_VISIBLE,
                    com.solthariel.pawchan.domain.settings.MAX_VISIBLE,
                )
            val showAll = section in list.sections.unfolded
            val visible = visibleSectionRows(rows, limit, list.searching || showAll)
            items(
                visible,
                key = { "${section.name}-${it.address.saveableKey}" },
                contentType = { "threads-row" },
            ) { row ->
                ThreadRow(
                    row = row,
                    current = section == ThreadsSection.PAGES && row.address == list.input.current,
                    onClick = { list.actions.open(section, row) },
                    onLongClick = { list.onMenu(ThreadsMenuTarget(section, row)) },
                    onClose = { list.actions.pages.close(row.address) }.takeIf { section == ThreadsSection.PAGES },
                    counterWidth = list.counterWidth.takeIf { section != ThreadsSection.PAGES },
                )
            }
            if (!list.searching && rows.size > limit) {
                item(key = "tail-${section.name}", contentType = "threads-tail") {
                    ThreadsTailRow(showAll, rows.size - limit) { list.sections.toggleTail(section) }
                }
            }
        }
    }
}

internal fun <T> visibleSectionRows(
    rows: List<T>,
    limit: Int,
    expanded: Boolean,
): List<T> = if (expanded) rows else rows.take(limit.coerceAtLeast(1))
