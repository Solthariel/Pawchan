package com.solthariel.pawchan.domain.boards

import kotlinx.coroutines.flow.StateFlow

/** Optional snapshots from a directory, including restored and background-refreshed data. */
interface BoardDirectorySnapshots {
    val snapshots: StateFlow<List<BoardDirectoryEntry>?>
}
