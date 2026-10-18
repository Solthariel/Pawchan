package com.solthariel.pawchan.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.solthariel.pawchan.domain.boards.BoardArchiveSnapshot
import com.solthariel.pawchan.domain.boards.BoardDetailsRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class ArchiveUiState(
    val snapshot: BoardArchiveSnapshot? = null,
    val pageIndex: Int = 0,
    val loading: Boolean = true,
    val failure: DomainFailure? = null,
)

internal class ArchiveViewModel(
    private val board: BoardAddress,
    private val repository: BoardDetailsRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ArchiveUiState())
    private var pages = listOf<Int?>(null)
    private var indexPages = emptyList<Int>()
    private var job: Job? = null
    val state = mutableState.asStateFlow()

    init {
        load(0)
    }

    fun refresh() {
        load(state.value.pageIndex)
    }

    fun previous() {
        if (!state.value.loading) load(state.value.pageIndex - 1)
    }

    fun next() {
        if (!state.value.loading) load(state.value.pageIndex + 1)
    }

    private fun load(index: Int) {
        if (index !in pages.indices) return
        job?.cancel()
        mutableState.value = state.value.copy(loading = true, failure = null)
        job =
            viewModelScope.launch {
                when (val result = repository.loadArchive(board, pages[index])) {
                    is DomainOutcome.Success -> {
                        if (index == 0) {
                            indexPages = result.value.pages
                            pages = listOf(null) + indexPages.dropLast(1).asReversed()
                        }
                        mutableState.value =
                            ArchiveUiState(result.value.copy(pages = indexPages), index, loading = false)
                    }

                    is DomainOutcome.Failure -> {
                        mutableState.value = state.value.copy(loading = false, failure = result.reason)
                    }
                }
            }
    }

    companion object {
        fun factory(
            board: BoardAddress,
            repository: BoardDetailsRepository,
        ): ViewModelProvider.Factory = viewModelFactory { initializer { ArchiveViewModel(board, repository) } }
    }
}
