package com.solthariel.pawchan.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

/** Экраны чтения: открытые из вкладки, они при уходе становятся точкой возврата, как цепочка каталога. */
internal val READING_ROUTES = setOf(AppRoute.THREAD, AppRoute.DOWNLOAD_ARCHIVE)

/** Верхний экран сохранённой цепочки: его открывает перезапуск, если приложение закрыли на вкладке. */
internal const val SUSPENDED_READING_ROUTE = "suspendedReadingRoute"

internal fun NavBackStackEntry.readingRoute(): String? =
    when (destination.route) {
        AppRoute.THREAD -> arguments?.toThreadAddress()?.let(AppRoute::thread)
        AppRoute.DOWNLOAD_ARCHIVE -> arguments?.getLong(DOWNLOAD_ARCHIVE_ARGUMENT)?.let(AppRoute::downloadArchive)
        AppRoute.ARCHIVE -> arguments?.toBoardAddress()?.let(AppRoute::archive)
        AppRoute.ARCHIVE_BROWSER -> arguments?.toBoardAddress()?.let(AppRoute::archiveBrowser)
        else -> null
    }

/**
 * Треды и архивы поверх вкладки (вместе с переходами по ссылкам между ними) сохраняются целиком и сменяют
 * прежнюю точку возврата: последний прочитанный тред важнее более старой цепочки каталога.
 */
internal fun NavController.suspendTabReading(base: NavBackStackEntry) {
    var popped = true
    while (popped && currentBackStackEntry?.destination?.route in CATALOG_TRANSIENT_ROUTES) popped = popBackStack()
    val run = (if (popped) tabReadingRun(base) else null) ?: return
    discardSuspendedCatalog()
    if (popBackStack(run.anchor.destination.id, inclusive = false, saveState = true)) {
        base.savedStateHandle[SUSPENDED_CATALOG_ROUTE] = run.first
        base.savedStateHandle[SUSPENDED_READING_ROUTE] = run.last
    }
}

private class TabReadingRun(
    val anchor: NavBackStackEntry,
    val first: String,
    val last: String,
)

/**
 * Экраны чтения над корнем вкладки: корень, первый и последний из них; null, если читали не во вкладке.
 * Весь стек навигации — закрытый API androidx, поэтому начало цепочки помнит сам экран чтения ([markReadingRun]).
 */
private fun NavController.tabReadingRun(base: NavBackStackEntry): TabReadingRun? {
    val top = currentBackStackEntry?.takeIf { it.destination.route in READING_ROUTES }
    val anchor =
        top
            ?.savedStateHandle
            ?.get<Int>(READING_RUN_ANCHOR)
            ?.let { id -> runCatching { getBackStackEntry(id) }.getOrNull() }
            ?.takeIf { it !== base && it.destination.route != AppRoute.CATALOG }
    val first = top?.savedStateHandle?.get<String>(READING_RUN_FIRST)
    val last = top?.readingRoute()
    return if (anchor != null && first != null && last != null) TabReadingRun(anchor, first, last) else null
}

/**
 * Вызывается при каждой смене экрана. Экран чтения запоминает, над каким экраном началась его цепочка
 * и какой тред в ней первый; следующий экран чтения по ссылке перенимает эти значения у предыдущего.
 */
internal fun NavController.markReadingRun() {
    val top = currentBackStackEntry?.takeIf { it.destination.route in READING_ROUTES } ?: return
    val below = previousBackStackEntry ?: return
    val handle = top.savedStateHandle
    if (below.destination.route in READING_ROUTES) {
        handle[READING_RUN_ANCHOR] = below.savedStateHandle.get<Int>(READING_RUN_ANCHOR)
        handle[READING_RUN_FIRST] = below.savedStateHandle.get<String>(READING_RUN_FIRST)
    } else {
        handle[READING_RUN_ANCHOR] = below.destination.id
        handle[READING_RUN_FIRST] = top.readingRoute()
    }
}

private const val READING_RUN_ANCHOR = "readingRunAnchor"
private const val READING_RUN_FIRST = "readingRunFirst"

/**
 * «Назад» с экрана чтения. Тред, открытый из вкладки, при этом не теряется: он становится точкой возврата
 * для «Каталога» и для «Назад» с корня вкладки. Остальные экраны просто закрываются.
 */
internal fun NavController.popReading(): Boolean {
    val top = currentBackStackEntry
    val base = entryOrNull(AppRoute.CATALOG)?.takeIf { activeTab() != null }
    val route = top?.takeIf { it.destination.route in READING_ROUTES }?.readingRoute()
    if (top == null || base == null || route == null) return top != null && popBackStack()
    discardSuspendedCatalog()
    val popped = popBackStack(top.destination.id, inclusive = true, saveState = true)
    if (popped) {
        base.savedStateHandle[SUSPENDED_CATALOG_ROUTE] = route
        base.savedStateHandle[SUSPENDED_READING_ROUTE] = route
    }
    return popped
}

/** Экран открыт внутри вкладки «Треды», «Доски» или «Настройки», а не в цепочке каталога. */
internal fun NavController.isInTab(): Boolean = activeTab() != null

internal const val DOWNLOAD_ARCHIVE_ARGUMENT = "archiveId"
