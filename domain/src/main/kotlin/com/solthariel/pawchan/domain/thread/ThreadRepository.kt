package com.solthariel.pawchan.domain.thread

import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot

interface ThreadRepository {
    suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot>
}
