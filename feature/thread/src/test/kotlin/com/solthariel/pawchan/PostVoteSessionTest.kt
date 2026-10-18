package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostRating
import com.solthariel.pawchan.domain.posting.PostingFailure
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.thread.PostVote
import com.solthariel.pawchan.domain.thread.PostVoteRepository
import com.solthariel.pawchan.thread.PostVoteSession
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PostVoteSessionTest {
    private val post = PostAddress(defaultThreadAddress(100), requireNotNull(PostId.parse(101)))
    private val rating = PostRating(likes = 3, dislikes = 1)

    @Test
    fun successfulLikeIncrementsCounterAndDisablesButtons() =
        runTest {
            val repository = FakeVotes(PostingOutcome.Success(Unit))
            val session = PostVoteSession(repository)

            assertNull(session.vote(post, PostVote.LIKE, rating))

            val state = session.state(post, rating)
            assertEquals(4, state.likes)
            assertEquals(1, state.dislikes)
            assertEquals(PostVote.LIKE, state.voted)
            assertFalse(state.enabled)
            assertEquals(4, session.state(post, PostRating(likes = 4, dislikes = 1)).likes)
        }

    @Test
    fun repeatedVoteIsNotSent() =
        runTest {
            val repository = FakeVotes(PostingOutcome.Success(Unit))
            val session = PostVoteSession(repository)

            session.vote(post, PostVote.DISLIKE, rating)
            session.vote(post, PostVote.LIKE, rating)

            assertEquals(listOf(PostVote.DISLIKE), repository.sent)
            assertEquals(2, session.state(post, rating).dislikes)
        }

    @Test
    fun rejectedVoteKeepsButtonsEnabledAndReturnsServerMessage() =
        runTest {
            val repository = FakeVotes(PostingOutcome.Failure(PostingFailure.Rejected("Вы уже голосовали")))
            val session = PostVoteSession(repository)

            assertEquals("Вы уже голосовали", session.vote(post, PostVote.LIKE, rating))

            val state = session.state(post, rating)
            assertEquals(3, state.likes)
            assertNull(state.voted)
            assertTrue(state.enabled)
        }

    private class FakeVotes(
        private val outcome: PostingOutcome<Unit>,
    ) : PostVoteRepository {
        val sent = mutableListOf<PostVote>()

        override suspend fun vote(
            post: PostAddress,
            vote: PostVote,
        ): PostingOutcome<Unit> {
            sent += vote
            return outcome
        }
    }
}
