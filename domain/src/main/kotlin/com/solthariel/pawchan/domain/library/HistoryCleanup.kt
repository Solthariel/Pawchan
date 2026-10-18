package com.solthariel.pawchan.domain.library

import com.solthariel.pawchan.domain.model.DomainOutcome

/** Clears visits without removing favorites, drafts, watched threads or saved copies. */
interface HistoryCleanup {
    suspend fun clearVisitHistory(): DomainOutcome<Unit>
}
