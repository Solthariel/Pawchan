package com.solthariel.pawchan.boards

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.ThreadAddress

/**
 * Что откроет Enter в поле поиска досок (код доски, ссылка на тред/пост); разбор ввода
 * принадлежит адаптеру сайта. Отдельной строки «Открыть …» в списке нет: доска из списка
 * открывается тапом, остальное — клавишей «Перейти».
 */
internal sealed interface BoardsAddressTarget {
    data class Board(
        val board: BoardAddress,
    ) : BoardsAddressTarget

    data class Thread(
        val address: ThreadAddress,
        val postId: Long?,
    ) : BoardsAddressTarget
}

internal data class BoardsAddressBindings(
    val resolve: (String) -> BoardsAddressTarget?,
    val open: (BoardsAddressTarget) -> Unit,
)
