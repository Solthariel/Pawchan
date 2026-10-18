package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostRating
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.posting.PostingFailure
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.thread.PostVote
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PostRatingTest {
    private val address =
        ThreadAddress(
            BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("news"))),
            requireNotNull(ThreadId.parse(100)),
        )
    private val decoder = ThreadPayloadDecoder(Json { ignoreUnknownKeys = true })

    @Test fun ratingIsDecodedOnlyOnBoardsWithLikes() {
        val enabled = decoder.decode(address, payload(true))
        assertEquals(
            PostRating(4553, 6119),
            enabled
                ?.posts
                ?.single()
                ?.content
                ?.rating,
        )
        val disabled = decoder.decode(address, payload(false))
        assertNull(
            disabled
                ?.posts
                ?.single()
                ?.content
                ?.rating,
        )
    }

    @Test fun voteRequestMatchesSiteScript() {
        val post = PostAddress(address, requireNotNull(PostId.parse(123)))
        assertEquals("https://2ch.su/api/like?board=news&num=123", postVoteRequest(post, PostVote.LIKE).url.toString())
        assertEquals(
            "https://2ch.su/api/dislike?board=news&num=123",
            postVoteRequest(post, PostVote.DISLIKE).url.toString(),
        )
        assertEquals("GET", postVoteRequest(post, PostVote.LIKE).method)
    }

    @Test fun voteResponseKeepsServerMessage() {
        assertEquals(PostingOutcome.Success(Unit), decodePostVoteResponse("""{"result":1}"""))
        assertEquals(
            PostingOutcome.Failure(PostingFailure.Rejected("Ошибка -4: Вы уже голосовали.")),
            decodePostVoteResponse("""{"result":0,"error":{"code":-4,"message":"Вы уже голосовали."}}"""),
        )
    }

    private fun payload(likes: Boolean) =
        """
{"board":{"enable_likes":$likes},
"threads":[{"posts":[{"num":100,"parent":0,"comment":"body","likes":4553,"dislikes":6119}]}]}
"""
}
