package com.solthariel.pawchan.threads

import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.workspace.WorkspaceState

/** Сворачиваемые секции экрана «Треды». */
internal enum class ThreadsSection(
    val title: String,
) {
    PAGES("Страницы"),
    FAVORITES("Избранные треды"),
    REPLIES("Ответы"),
}

/**
 * Квадратик справа от треда: число новых постов (или ответов), `×` — тред удалён,
 * `?` — проверка не удалась, точка — идёт проверка. В заголовке секции нули не показываются,
 * в строках «Избранных тредов» и «Ответов» ячейка есть всегда — ноль приглушён ([cellLabel]).
 */
internal data class ThreadCounter(
    val count: Int = 0,
    val running: Boolean = false,
    val deleted: Boolean = false,
    val error: Boolean = false,
) {
    val visible: Boolean
        get() = count > 0 || running || deleted || error

    val label: String
        get() {
            val number =
                when {
                    count >= COUNTER_THOUSAND -> "${count / COUNTER_THOUSAND}K+"
                    count > 0 -> count.toString()
                    else -> ""
                }
            return when {
                deleted -> "×"
                error -> "$number?"
                else -> number
            }
        }

    /** Текст постоянной ячейки строки: ноль пишется явно; во время проверки без числа — точка. */
    val cellLabel: String
        get() = label.ifEmpty { if (running) "" else "0" }
}

/** Строка треда: только название и счётчик. */
internal data class ThreadRowUi(
    val address: ThreadAddress,
    val title: String,
    val counter: ThreadCounter = ThreadCounter(),
    /** Счётчик ответов вам — выделяется цветом. */
    val reply: Boolean = false,
)

internal fun threadTitle(
    address: ThreadAddress,
    title: String?,
): String = title?.trim()?.takeIf(String::isNotEmpty) ?: "Тред №${address.thread.value}"

/** Открытые треды, последний открытый — сверху. */
internal fun pageRows(state: WorkspaceState): List<ThreadRowUi> =
    state.tabs.asReversed().map { ThreadRowUi(it.address, threadTitle(it.address, it.title)) }

/**
 * Треды, где вы писали: попадают сюда сами после первого своего поста. Счётчик — непрочитанные ответы вам.
 * Утонувший тред без непрочитанных ответов уходит сам, убранный вручную — до следующего своего поста.
 */
internal fun replyRows(state: WorkspaceState): List<ThreadRowUi> {
    val own = state.posts.filter { it.own }.groupBy { it.post.address.thread }
    return own
        .filterKeys { it !in state.marks.dismissedReplies }
        .mapNotNull { (address, posts) ->
            val unread = state.replies.count { it.unread && it.post.address.thread == address }
            if (unread == 0 && posts.none { it.tracking }) return@mapNotNull null
            val title = posts.firstNotNullOfOrNull { it.title }
            val latest = posts.maxOf { it.post.address.post.value }
            ThreadRowUi(address, threadTitle(address, title), ThreadCounter(unread), reply = true) to latest
        }.sortedWith(
            compareByDescending<Pair<ThreadRowUi, Long>> { it.first.counter.count > 0 }.thenByDescending { it.second },
        ).map { it.first }
}

/** Всего непрочитанных ответов в «Ответах» — бейдж на кнопке «Треды». */
internal fun unreadReplies(state: WorkspaceState): Int = replyRows(state).sumOf { it.counter.count }

/** Первый непрочитанный ответ в треде — к нему прокручивается тред при открытии из «Ответов». */
internal fun firstUnreadReply(
    state: WorkspaceState,
    address: ThreadAddress,
): Long? =
    state.replies
        .filter { it.unread && it.post.address.thread == address }
        .minOfOrNull { it.post.address.post.value }

/** Новые посты избранного треда: живые посты после последнего захода. Без отметки захода — ноль. */
internal fun newPostsCount(
    snapshot: ThreadSnapshot?,
    seen: Long?,
): Int {
    if (snapshot == null || seen == null) return 0
    return snapshot.posts.count { it.address.post.value > seen && it.address.post !in snapshot.deletedPosts }
}

private const val COUNTER_THOUSAND = 1000
