package com.solthariel.pawchan.infrastructure

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.posting.NewThreadDraft
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.posting.PostingProgressObserver
import com.solthariel.pawchan.domain.posting.PostingRepository
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import com.solthariel.pawchan.domain.posting.ReplyDraft
import com.solthariel.pawchan.domain.statistics.UsageMetric
import com.solthariel.pawchan.domain.statistics.UsageStatisticsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Counts the prepared files actually accepted by the server, not local preview attachments. */
internal class UsagePostingRepository(
    private val delegate: PostingRepository,
    private val stats: UsageStatisticsRepository,
) : PostingRepository by delegate {
    override suspend fun submitReply(
        address: ThreadAddress,
        draft: ReplyDraft,
        observer: PostingProgressObserver,
    ): PostingOutcome<PostId> {
        val outcome = delegate.submitReply(address, draft, observer)
        if (outcome is PostingOutcome.Success) {
            recordSubmission("$address/${outcome.value.value}", false, draft.attachments)
        }
        return outcome
    }

    override suspend fun createThread(
        board: BoardAddress,
        draft: NewThreadDraft,
        observer: PostingProgressObserver,
    ): PostingOutcome<ThreadAddress> {
        val outcome = delegate.createThread(board, draft, observer)
        if (outcome is PostingOutcome.Success) {
            recordSubmission("${outcome.value}/${outcome.value.thread.value}", true, draft.attachments)
        }
        return outcome
    }

    private suspend fun recordSubmission(
        identity: String,
        created: Boolean,
        files: List<ReplyAttachment>,
    ) {
        stats.record(UsageMetric.POSTS_SENT, identity = identity)
        stats.record(if (created) UsageMetric.THREADS_CREATED else UsageMetric.REPLIES_SENT, identity = identity)
        files.forEach { file ->
            val metric =
                when {
                    file.mimeType?.startsWith("image/", ignoreCase = true) == true -> UsageMetric.IMAGES_SENT
                    file.mimeType?.startsWith("video/", ignoreCase = true) == true -> UsageMetric.VIDEOS_SENT
                    else -> null
                }
            if (metric != null) {
                val fileIdentity =
                    withContext(Dispatchers.Default) {
                        com.solthariel.pawchan.domain.statistics
                            .usageFileIdentity(file.content)
                    }
                stats.record(metric, identity = fileIdentity)
            }
        }
    }
}
