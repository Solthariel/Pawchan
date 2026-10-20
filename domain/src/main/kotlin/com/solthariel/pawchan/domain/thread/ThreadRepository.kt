package com.solthariel.pawchan.domain.thread

import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot

interface ThreadRepository {
    /** A confirmed deletion survives transient failures and process recreation. */
    suspend fun isThreadDeleted(address: ThreadAddress): Boolean = false

    suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot>
}
