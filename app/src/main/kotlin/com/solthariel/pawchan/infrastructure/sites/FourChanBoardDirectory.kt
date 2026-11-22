package com.solthariel.pawchan.infrastructure.sites

import com.solthariel.pawchan.domain.boards.BoardDirectoryRepository
import com.solthariel.pawchan.domain.model.DomainOutcome

/** The public API does not publish topic categories; this adapter supplies the site-directory groups. */
internal class FourChanBoardDirectory(
    private val delegate: BoardDirectoryRepository,
) : BoardDirectoryRepository by delegate {
    override suspend fun loadBoards(forceRefresh: Boolean) =
        when (val result = delegate.loadBoards(forceRefresh)) {
            is DomainOutcome.Failure -> {
                result
            }

            is DomainOutcome.Success -> {
                DomainOutcome.Success(
                    result.value.map { board ->
                        if (board.address.site.value != "4chan") {
                            board
                        } else {
                            board.copy(category = categoryFor(board.address.board.value, board.category))
                        }
                    },
                )
            }
        }
}

internal fun categoryFor(
    code: String,
    original: String,
): String =
    GROUPS.entries.firstOrNull { code in it.value }?.key
        ?: if (original == "Взрослым") "Остальное (18+)" else "Остальное"

private val GROUPS =
    linkedMapOf(
        "Японская культура" to setOf("a", "c", "w", "m", "cgl", "cm", "f", "n", "jp", "vt"),
        "Видеоигры" to setOf("v", "vg", "vm", "vmg", "vp", "vr", "vrpg", "vst"),
        "Интересы" to setOf("co", "g", "tv", "k", "o", "an", "tg", "sp", "xs", "pw", "sci", "his", "int", "out", "toy"),
        "Творчество" to setOf("i", "po", "p", "ck", "ic", "wg", "lit", "mu", "fa", "3", "gd", "diy", "wsg", "qst"),
        "Другое" to setOf("biz", "trv", "fit", "x", "adv", "lgbt", "mlp", "news", "wsr", "vip"),
        "Разное (18+)" to setOf("b", "r9k", "pol", "bant", "soc", "s4s", "trash"),
        "Взрослым (18+)" to setOf("s", "hc", "hm", "h", "e", "u", "d", "y", "t", "hr", "gif", "aco", "r"),
    )
