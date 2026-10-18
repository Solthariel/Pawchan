package com.solthariel.pawchan.domain.thread

import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.posting.PostingOutcome

interface ReportingThreadRepository : ThreadRepository {
    suspend fun reportPost(
        address: ThreadAddress,
        post: PostId,
        comment: String,
    ): PostingOutcome<Unit>
}
