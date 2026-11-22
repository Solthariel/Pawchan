package com.solthariel.pawchan.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.solthariel.pawchan.boards.BoardLookupContent
import com.solthariel.pawchan.boards.BoardLookupSource
import com.solthariel.pawchan.boards.prepareLookup
import com.solthariel.pawchan.domain.model.BoardAddress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun rememberLookupContent(
    source: BoardLookupSource,
    board: BoardAddress,
    query: String,
    site: com.solthariel.pawchan.domain.model.SiteId = board.site,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
): androidx.compose.runtime.State<BoardLookupContent?> =
    key(board, site) {
        val initial = remember { if (query.isBlank()) source.prepareLookup(board, query, site) else null }
        produceState<BoardLookupContent?>(initial, source, query, dispatcher) {
            value = withContext(dispatcher) { source.prepareLookup(board, query, site) }
        }
    }
