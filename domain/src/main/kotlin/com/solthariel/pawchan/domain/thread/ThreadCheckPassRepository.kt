package com.solthariel.pawchan.domain.thread

import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One short-lived check pass. Never retained by foreground screens or across worker runs. */
class ThreadCheckPassRepository(
    private val source: ThreadRepository,
) : ThreadRepository {
    private val mutex = Mutex()
    private val outcomes = mutableMapOf<ThreadAddress, DomainOutcome<ThreadSnapshot>>()

    override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> =
        mutex.withLock {
            outcomes[address] ?: source.loadThread(address).also { outcomes[address] = it }
        }
}
