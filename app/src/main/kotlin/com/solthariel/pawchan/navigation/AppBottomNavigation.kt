package com.solthariel.pawchan.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.solthariel.pawchan.domain.model.BoardAddress

/**
 * Стек экранов: в основании всегда каталог текущей доски. Выше лежит либо цепочка каталога (архив, тред,
 * тред по ссылке), либо одна из вкладок «Треды», «Доски», «Настройки» со своими вложенными экранами.
 *
 * Уход со вкладки сохраняет её стек (`saveState`) вместе с ViewModel и прокруткой, возврат восстанавливает
 * его (`restoreState`) — каждая кнопка независима. Каталог не снимается со стека, поэтому сохраняет
 * загруженный список и прокрутку при любых переключениях. При уходе из цепочки каталога её стек
 * сохраняется и восстанавливается при возврате на кнопку «Каталог».
 *
 * Тред или архив, открытые из вкладки, тоже становятся точкой возврата: при уходе с них (другая кнопка,
 * повторное нажатие вкладки, «Назад») они сменяют прежнюю цепочку каталога, см. ReadingReturn.kt.
 */
internal val TAB_ROUTES = setOf(AppRoute.LIBRARY, AppRoute.BOARDS, AppRoute.SETTINGS)

internal val CATALOG_TRANSIENT_ROUTES = setOf(AppRoute.NEW_THREAD, AppRoute.CHALLENGE)

/** Экраны, которые не сохраняются вместе со стеком вкладки. */
private val TRANSIENT_ROUTES = READING_ROUTES + CATALOG_TRANSIENT_ROUTES

/**
 * «Каталог» и «Назад» с корня вкладки: открывается основание стека. [keepReading] = false — выбор доски:
 * тред, открытый из вкладки, тогда не становится точкой возврата.
 */
internal fun NavController.navigateCatalogBase(keepReading: Boolean = true) {
    val base = entryOrNull(AppRoute.CATALOG) ?: return
    val tab = activeTab() != null
    if (tab && keepReading) suspendTabReading(base)
    // Тред, открытый из вкладки, в её сохранённый стек не попадает.
    var popped = true
    while (tab && popped && currentBackStackEntry?.destination?.route in TRANSIENT_ROUTES) popped = popBackStack()
    if (currentBackStackEntry !== base) popBackStack(base.destination.id, inclusive = false, saveState = tab)
}

/** Кнопка вкладки: повторное нажатие возвращает к её корню, переход — восстанавливает её стек. */
internal fun NavController.navigateTab(route: String) {
    val current = activeTab()
    if (current?.destination?.route == route) {
        entryOrNull(AppRoute.CATALOG)?.let(::suspendTabReading)
        popBackStack(current.destination.id, inclusive = false)
        return
    }
    if (current == null) suspendCatalogStack() else navigateCatalogBase()
    navigate(route) {
        launchSingleTop = true
        restoreState = true
    }
}

/** Переключение на каталог восстанавливает его цепочку; повторное нажатие возвращает к корню. */
internal fun NavController.navigateCatalogTab() {
    val base = entryOrNull(AppRoute.CATALOG) ?: return
    if (activeTab() == null) {
        discardSuspendedCatalog()
        navigateCatalogBase()
    } else {
        navigateCatalogBase()
        val route = base.savedStateHandle.remove<String>(SUSPENDED_CATALOG_ROUTE)
        if (route != null) navigate(route) { restoreState = true }
        base.savedStateHandle.remove<String>(SUSPENDED_READING_ROUTE)
    }
}

/** Save only the catalog-owned chain. Temporary challenge/composer destinations are not restored. */
private fun NavController.suspendCatalogStack() {
    val base = entryOrNull(AppRoute.CATALOG) ?: return
    while (currentBackStackEntry?.destination?.route in CATALOG_TRANSIENT_ROUTES) {
        if (!popBackStack()) break
    }
    val route = if (currentBackStackEntry?.destination?.route in CATALOG_TRANSIENT_ROUTES) null else catalogStackRoute()
    val top = currentBackStackEntry?.readingRoute()
    if (route != null && popBackStack(base.destination.id, inclusive = false, saveState = true)) {
        base.savedStateHandle[SUSPENDED_CATALOG_ROUTE] = route
        base.savedStateHandle[SUSPENDED_READING_ROUTE] = top ?: route
    }
}

/** Any popped destination restores the whole saved chain: archive if present, otherwise a reading screen. */
private fun NavController.catalogStackRoute(): String? =
    entryOrNull(AppRoute.ARCHIVE_BROWSER)?.arguments?.toBoardAddress()?.let(AppRoute::archiveBrowser)
        ?: entryOrNull(AppRoute.ARCHIVE)?.arguments?.toBoardAddress()?.let(AppRoute::archive)
        ?: entryOrNull(AppRoute.THREAD)?.readingRoute()
        ?: entryOrNull(AppRoute.DOWNLOAD_ARCHIVE)?.readingRoute()

/** A board change invalidates the suspended catalog chain, including its saved ViewModels. */
internal fun NavController.discardSuspendedCatalog() {
    val handle = entryOrNull(AppRoute.CATALOG)?.savedStateHandle ?: return
    handle.remove<String>(SUSPENDED_READING_ROUTE)
    val route = handle.remove<String>(SUSPENDED_CATALOG_ROUTE) ?: return
    val pattern =
        when {
            route.startsWith("archive-browser/") -> AppRoute.ARCHIVE_BROWSER
            route.startsWith("archive/") -> AppRoute.ARCHIVE
            route.startsWith("download-archive/") -> AppRoute.DOWNLOAD_ARCHIVE
            else -> AppRoute.THREAD
        }
    clearBackStack(pattern)
}

internal const val SUSPENDED_CATALOG_ROUTE = "suspendedCatalogRoute"

/** Выбор доски заменяет основание стека; сохранённые стеки вкладок не трогаются. */
internal fun NavController.navigateBoard(board: BoardAddress) {
    discardSuspendedCatalog()
    navigateCatalogBase(keepReading = false)
    val base = entryOrNull(AppRoute.CATALOG) ?: return
    if (base.arguments?.toBoardAddress() == board) return
    navigate(AppRoute.catalog(board)) {
        popUpTo(base.destination.id) { inclusive = true }
    }
}

/**
 * Первый запуск: последний сохранённый экран открывается поверх каталога. Если это вкладка, тред, который
 * читали до неё ([reading]), остаётся точкой возврата для «Каталога» и «Назад».
 */
internal fun NavController.openRestoredRoute(
    route: String?,
    reading: String? = null,
) {
    val onlyBase = previousBackStackEntry == null && currentBackStackEntry?.destination?.route == AppRoute.CATALOG
    if (route == null || !onlyBase) return
    when {
        route.startsWith("archive-browser/") -> {
            navigate(route)
        }

        route in TAB_ROUTES -> {
            currentBackStackEntry?.savedStateHandle?.let { handle ->
                if (reading != null) {
                    handle[SUSPENDED_CATALOG_ROUTE] = reading
                    handle[SUSPENDED_READING_ROUTE] = reading
                }
            }
            navigateTab(route)
        }

        route.startsWith("thread/") -> {
            AppRoute.threadAddress(route)?.let(::navigateThread)
        }

        route.startsWith("archive/") || route.startsWith("download-archive/") -> {
            navigate(route)
        }
    }
}

/** Доска основания стека при запуске: доска последнего экрана, затем доска, которую читали, иначе домашняя. */
internal fun restoredBaseBoard(
    route: String?,
    home: BoardAddress,
    lastBase: String? = null,
): BoardAddress {
    val board =
        route?.let { restored ->
            AppRoute.catalogBoard(restored)
                ?: AppRoute.threadAddress(restored)?.board
                ?: AppRoute.catalogBoard(restored.replaceFirst("archive/", "catalog/"))
                ?: AppRoute.catalogBoard(restored.replaceFirst("archive-browser/", "catalog/"))
        }
    return board ?: lastBase?.let(AppRoute::catalogBoard) ?: home
}

/** Активная вкладка: сохранённые стеки других вкладок в текущем стеке не лежат. */
internal fun NavController.activeTab(): NavBackStackEntry? = TAB_ROUTES.firstNotNullOfOrNull(::entryOrNull)

internal fun NavController.entryOrNull(route: String): NavBackStackEntry? =
    try {
        getBackStackEntry(route)
    } catch (_: IllegalArgumentException) {
        null
    }
