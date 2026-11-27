package com.solthariel.pawchan.domain.settings

/** Где что стоит: кнопки нижней панели и разделы «Тредов». По умолчанию всё как было до настройки. */
data class NavigationLayout(
    val bottomBar: List<BottomBarButton> = BottomBarButton.entries,
    /** Разделы экрана «Треды» сверху вниз. */
    val threads: List<ThreadsHubItem> = ThreadsHubItem.entries,
    /** Ссылки, убранные с экрана в меню ⋮. */
    val threadsInMenu: Set<ThreadsHubItem> = emptySet(),
) {
    fun normalized(): NavigationLayout =
        NavigationLayout(
            normalizedBottomBarOrder(bottomBar),
            (threads + ThreadsHubItem.entries).distinct(),
            threadsInMenu.filterTo(linkedSetOf()) { it.movableToMenu },
        )
}

/** Разделы «Тредов»; [id] хранится в настройках. В ⋮ можно убрать только ссылки. */
enum class ThreadsHubItem(
    val id: String,
    val movableToMenu: Boolean,
) {
    PAGES("pages", false),
    FAVORITES("favorites", false),
    REPLIES("replies", false),
    HISTORY("history", true),
    COPIES("copies", true),
    DOWNLOADS("downloads", true),
}

fun parseThreadsHubItems(value: String): List<ThreadsHubItem> =
    value
        .split(',')
        .mapNotNull { id -> ThreadsHubItem.entries.firstOrNull { it.id == id.trim() } }
        .distinct()

fun Collection<ThreadsHubItem>.threadsHubValue(): String = joinToString(",") { it.id }
