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
 * загруженный список и прокрутку при любых переключениях. Тред при уходе закрывается без сохранения стека:
 * его снимок и позиция остаются в рабочем пространстве («Страницы»), и при возврате тред открывается
 * на том же месте без загрузки, как в Dashchan.
 */
internal val TAB_ROUTES = setOf(AppRoute.LIBRARY, AppRoute.BOARDS, AppRoute.SETTINGS)

/** Экраны, которые не сохраняются вместе со стеком вкладки. */
private val TRANSIENT_ROUTES = setOf(AppRoute.THREAD, AppRoute.NEW_THREAD, AppRoute.CHALLENGE)

/** «Каталог» и «Назад» с корня вкладки: открывается основание стека. */
internal fun NavController.navigateCatalogBase() {
    val base = entryOrNull(AppRoute.CATALOG) ?: return
    val tab = activeTab() != null
    // Тред, открытый из вкладки, в её сохранённый стек не попадает.
    var popped = true
    while (tab && popped && currentBackStackEntry?.destination?.route in TRANSIENT_ROUTES) popped = popBackStack()
    if (currentBackStackEntry !== base) popBackStack(base.destination.id, inclusive = false, saveState = tab)
}

/** Кнопка вкладки: повторное нажатие возвращает к её корню, переход — восстанавливает её стек. */
internal fun NavController.navigateTab(route: String) {
    val current = activeTab()
    if (current?.destination?.route == route) {
        popBackStack(current.destination.id, inclusive = false)
        return
    }
    navigateCatalogBase()
    navigate(route) {
        launchSingleTop = true
        restoreState = true
    }
}

/** Выбор доски заменяет основание стека; сохранённые стеки вкладок не трогаются. */
internal fun NavController.navigateBoard(board: BoardAddress) {
    navigateCatalogBase()
    val base = entryOrNull(AppRoute.CATALOG) ?: return
    if (base.arguments?.toBoardAddress() == board) return
    navigate(AppRoute.catalog(board)) {
        popUpTo(base.destination.id) { inclusive = true }
    }
}

/** Первый запуск: последний сохранённый экран открывается поверх каталога. */
internal fun NavController.openRestoredRoute(route: String?) {
    val onlyBase = previousBackStackEntry == null && currentBackStackEntry?.destination?.route == AppRoute.CATALOG
    if (route == null || !onlyBase) return
    when {
        route in TAB_ROUTES -> navigateTab(route)
        route.startsWith("thread/") -> AppRoute.threadAddress(route)?.let(::navigateThread)
        route.startsWith("archive/") -> navigate(route)
    }
}

/** Доска основания стека при запуске: доска последнего экрана, иначе домашняя. */
internal fun restoredBaseBoard(
    route: String?,
    home: BoardAddress,
): BoardAddress {
    val board =
        route?.let { restored ->
            AppRoute.catalogBoard(restored)
                ?: AppRoute.threadAddress(restored)?.board
                ?: AppRoute.catalogBoard(restored.replaceFirst("archive/", "catalog/"))
        }
    return board ?: home
}

/** Активная вкладка: сохранённые стеки других вкладок в текущем стеке не лежат. */
private fun NavController.activeTab(): NavBackStackEntry? = TAB_ROUTES.firstNotNullOfOrNull(::entryOrNull)

private fun NavController.entryOrNull(route: String): NavBackStackEntry? =
    try {
        getBackStackEntry(route)
    } catch (_: IllegalArgumentException) {
        null
    }
