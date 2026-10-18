package com.solthariel.pawchan.data.database

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomThreadWatchRepositoryTest {
    @Test
    fun storesUpdatesAndRemovesSubscription() =
        runTest {
            val repository = RoomThreadWatchRepository(FakeThreadWatchDao())
            val address = address()

            repository.enable(
                address = address,
                subject = "Тестовый тред",
                lastKnownPostId = requireNotNull(PostId.parse(43L)),
                enabledAtEpochMillis = 100L,
            )
            repository.updateCheckpoint(address, requireNotNull(PostId.parse(44L)))
            repository.updateCheckpoint(address, requireNotNull(PostId.parse(43L)))

            val loaded = repository.load(address) as DomainOutcome.Success
            assertEquals(44L, loaded.value?.lastKnownPostId?.value)
            assertEquals(1, (repository.loadAll() as DomainOutcome.Success).value.size)

            repository.disable(address)
            assertEquals(null, (repository.load(address) as DomainOutcome.Success).value)
        }

    private fun address(): ThreadAddress =
        ThreadAddress(
            board =
                BoardAddress(
                    site = requireNotNull(SiteId.parse("dvach")),
                    board = requireNotNull(BoardId.parse("b")),
                ),
            thread = requireNotNull(ThreadId.parse(42L)),
        )
}

private class FakeThreadWatchDao : ThreadWatchDao {
    private var stored: ThreadWatchEntity? = null

    override suspend fun upsert(entry: ThreadWatchEntity) {
        stored = entry
    }

    override suspend fun delete(
        site: String,
        board: String,
        threadId: Long,
    ) {
        stored = null
    }

    override suspend fun updateCheckpoint(
        site: String,
        board: String,
        threadId: Long,
        postId: Long,
    ) {
        val current = stored?.lastKnownPostId
        if (current == null || current < postId) {
            stored = stored?.copy(lastKnownPostId = postId)
        }
    }

    override suspend fun load(
        site: String,
        board: String,
        threadId: Long,
    ): ThreadWatchEntity? = stored

    override suspend fun loadAll(): List<ThreadWatchEntity> = listOfNotNull(stored)
}
