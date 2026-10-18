package com.solthariel.pawchan.library

import com.solthariel.pawchan.domain.library.ThreadLibraryEntry
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress

internal sealed interface LibraryUiState {
    data object Loading : LibraryUiState

    data class Content(
        val entries: List<LibraryEntryUi>,
    ) : LibraryUiState {
        val favorites: List<LibraryEntryUi> = entries.filter(LibraryEntryUi::favorite)
    }

    data object Error : LibraryUiState
}

internal data class LibraryEntryUi(
    val address: ThreadAddress,
    val subject: String?,
    val lastVisitedAtEpochMillis: Long,
    val favorite: Boolean,
) {
    val threadId: Long
        get() = address.thread.value

    val board: String
        get() = address.board.board.value
}

internal fun DomainOutcome<List<ThreadLibraryEntry>>.toLibraryUiState(): LibraryUiState =
    when (this) {
        is DomainOutcome.Success -> {
            LibraryUiState.Content(
                entries =
                    value.map { entry ->
                        LibraryEntryUi(
                            address = entry.address,
                            subject = entry.subject,
                            lastVisitedAtEpochMillis = entry.lastVisitedAtEpochMillis,
                            favorite = entry.favorite,
                        )
                    },
            )
        }

        is DomainOutcome.Failure -> {
            LibraryUiState.Error
        }
    }
