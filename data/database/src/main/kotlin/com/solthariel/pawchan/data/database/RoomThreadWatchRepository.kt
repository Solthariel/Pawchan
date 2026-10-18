package com.solthariel.pawchan.data.database

import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.watch.ThreadWatchRepository
import com.solthariel.pawchan.domain.watch.ThreadWatchSubscription

class RoomThreadWatchRepository internal constructor(
    private val dao: ThreadWatchDao,
) : ThreadWatchRepository {
    override suspend fun enable(
        address: ThreadAddress,
        subject: String?,
        lastKnownPostId: PostId?,
        enabledAtEpochMillis: Long,
    ): DomainOutcome<Unit> =
        storageCall {
            dao.upsert(
                ThreadWatchEntity(
                    site = address.site.value,
                    board = address.board.board.value,
                    threadId = address.thread.value,
                    subject = subject,
                    lastKnownPostId = lastKnownPostId?.value,
                    enabledAtEpochMillis = enabledAtEpochMillis,
                ),
            )
        }

    override suspend fun disable(address: ThreadAddress): DomainOutcome<Unit> =
        storageCall {
            dao.delete(address.site.value, address.board.board.value, address.thread.value)
        }

    override suspend fun updateCheckpoint(
        address: ThreadAddress,
        lastKnownPostId: PostId,
    ): DomainOutcome<Unit> =
        storageCall {
            dao.updateCheckpoint(
                address.site.value,
                address.board.board.value,
                address.thread.value,
                lastKnownPostId.value,
            )
        }

    override suspend fun load(address: ThreadAddress): DomainOutcome<ThreadWatchSubscription?> =
        storageCall {
            dao
                .load(address.site.value, address.board.board.value, address.thread.value)
                ?.toDomain()
        }

    override suspend fun loadAll(): DomainOutcome<List<ThreadWatchSubscription>> =
        storageCall {
            dao.loadAll().mapNotNull(ThreadWatchEntity::toDomain)
        }
}

private fun ThreadWatchEntity.toDomain(): ThreadWatchSubscription? {
    val address = storedThreadAddress(site, board, threadId)
    val postId = lastKnownPostId?.let(PostId::parse)
    return if (address != null) {
        ThreadWatchSubscription(
            address = address,
            subject = subject,
            lastKnownPostId = postId,
            enabledAtEpochMillis = enabledAtEpochMillis,
        )
    } else {
        null
    }
}
