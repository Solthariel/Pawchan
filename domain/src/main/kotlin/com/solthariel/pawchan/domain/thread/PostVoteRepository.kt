package com.solthariel.pawchan.domain.thread

import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.posting.PostingOutcome

enum class PostVote { LIKE, DISLIKE }

/** Необязательная возможность сайта: оценка поста. Доступна только там, где пост несёт PostRating. */
interface PostVoteRepository {
    suspend fun vote(
        post: PostAddress,
        vote: PostVote,
    ): PostingOutcome<Unit>
}
