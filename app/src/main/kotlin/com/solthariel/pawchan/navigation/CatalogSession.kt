package com.solthariel.pawchan.navigation

import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.lifecycle.ViewModelStoreOwner
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.ThreadAddress

internal data class CatalogSession(
    val currentBoard: () -> BoardAddress?,
    val currentThread: () -> ThreadAddress?,
    val states: SaveableStateHolder,
    val archiveOwner: ViewModelStoreOwner,
    val boardLists: MutableMap<String, com.solthariel.pawchan.boards.BoardsListUi>,
)
