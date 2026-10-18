package com.solthariel.pawchan.thread

import com.solthariel.pawchan.domain.library.ThreadLibraryRepository
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot

/** Borrowed ports; the ViewModel's caller coroutine owns observation and visit persistence. */
internal class ThreadVisitRecorder(
    private val address: ThreadAddress,
    private val library: ThreadLibraryRepository,
    private val workspaceEvents: ThreadWorkspaceEvents?,
    private val nowMillis: () -> Long,
) {
    suspend fun record(outcome: DomainOutcome<ThreadSnapshot>) {
        if (outcome is DomainOutcome.Success) {
            workspaceEvents?.observe(outcome.value)
            library.recordVisit(
                address = address,
                subject = outcome.value.subject,
                visitedAtEpochMillis = nowMillis(),
            )
        }
    }
}
