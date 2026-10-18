package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.thread.ThreadRepository

internal class IntegrationThreadRepository(
    vararg outcomes: DomainOutcome<ThreadSnapshot>,
) : ThreadRepository {
    private val outcomes = ArrayDeque(outcomes.toList())

    var requests: Int = 0
        private set

    override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> {
        requests += 1
        return outcomes.removeFirst()
    }
}
