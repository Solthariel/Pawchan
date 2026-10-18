package com.solthariel.pawchan.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.solthariel.pawchan.domain.library.ThreadLibraryRepository
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class LibraryViewModel(
    private val repository: ThreadLibraryRepository,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val mutableState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    private var loadJob: Job? = null
    private val favoriteJobs = mutableMapOf<ThreadAddress, Job>()

    val state: StateFlow<LibraryUiState> = mutableState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob =
            viewModelScope.launch {
                mutableState.value = repository.loadEntries().toLibraryUiState()
            }
    }

    fun setFavorite(
        entry: LibraryEntryUi,
        favorite: Boolean,
    ) {
        val content = mutableState.value as? LibraryUiState.Content ?: return
        val current = content.entries.firstOrNull { it.address == entry.address } ?: return
        if (current.favorite != favorite) {
            mutableState.value = content.withFavorite(entry.address, favorite)
            favoriteJobs.remove(entry.address)?.cancel()
            favoriteJobs[entry.address] =
                viewModelScope.launch {
                    val outcome =
                        repository.setFavorite(
                            address = entry.address,
                            subject = entry.subject,
                            favorite = favorite,
                            updatedAtEpochMillis = nowMillis(),
                        )
                    if (outcome is DomainOutcome.Failure) {
                        val current = mutableState.value as? LibraryUiState.Content
                        val currentEntry = current?.entries?.firstOrNull { it.address == entry.address }
                        if (currentEntry?.favorite == favorite) {
                            mutableState.value = current.withFavorite(entry.address, !favorite)
                        }
                    }
                }
        }
    }

    /** Избранное по адресу — для тредов, которых может не быть в загруженном списке. */
    suspend fun setFavoriteAddress(
        address: ThreadAddress,
        subject: String?,
        favorite: Boolean,
    ) {
        repository.setFavorite(address, subject, favorite, nowMillis())
        refresh()
    }

    companion object {
        fun factory(repository: ThreadLibraryRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    LibraryViewModel(repository)
                }
            }
    }
}

private fun LibraryUiState.Content.withFavorite(
    address: ThreadAddress,
    favorite: Boolean,
): LibraryUiState.Content =
    copy(
        entries =
            entries.map { entry ->
                if (entry.address == address) {
                    entry.copy(favorite = favorite)
                } else {
                    entry
                }
            },
    )
