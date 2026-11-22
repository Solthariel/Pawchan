package com.solthariel.pawchan.arhivach

internal fun ArchiveBrowseState.canLoadNextPage(): Boolean {
    val hasAvailablePage = list?.hasNext == true && !loadingNextPage
    return hasAvailablePage && !load.loading && selectedThread == null
}

internal data class ArchiveNextPageRequest(
    val startPage: Int,
    val page: Int,
    val tags: List<Long>,
) {
    fun matches(state: ArchiveBrowseState): Boolean {
        val samePosition = state.startPage == startPage && state.page == page - 1
        val sameTags = state.tags.map { it.id } == tags
        return samePosition && sameTags && state.selectedThread == null
    }
}
