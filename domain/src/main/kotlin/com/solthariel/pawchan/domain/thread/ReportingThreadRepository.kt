package com.solthariel.pawchan.domain.thread

import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.posting.PostingOutcome

interface ReportingThreadRepository : ThreadRepository {
    fun supportsReporting(address: ThreadAddress): Boolean = true

    suspend fun reportPost(
        address: ThreadAddress,
        post: PostId,
        comment: String,
    ): PostingOutcome<Unit>
}
