package com.solthariel.pawchan.domain.thread

import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/** Shared guard for foreground, favorites and workers; only final NotFound is terminal. */
class MissingAwareThreadRepository(
    private val remote: ReportingThreadRepository,
    private val deleted: ThreadDeletionStore,
) : ReportingThreadRepository by remote {
    private val known = ConcurrentHashMap.newKeySet<ThreadAddress>()
    private val requests = ConcurrentHashMap<ThreadAddress, Mutex>()

    override suspend fun isThreadDeleted(address: ThreadAddress): Boolean =
        address in known || deleted.contains(address).also { if (it) known.add(address) }

    override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> =
        requests.getOrPut(address) { Mutex() }.withLock {
            if (isThreadDeleted(address)) return@withLock DomainOutcome.Failure(DomainFailure.NotFound)
            remote.loadThread(address).also { outcome ->
                if (outcome == DomainOutcome.Failure(DomainFailure.NotFound)) {
                    known.add(address)
                    deleted.markDeleted(address)
                }
            }
        }
}
