package com.solthariel.pawchan.domain.watch

import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.thread.ThreadRepository

sealed interface ThreadWatchResult {
    data class Unchanged(
        val snapshot: ThreadSnapshot,
        val checkpoint: PostId?,
    ) : ThreadWatchResult

    data class NewPosts(
        val snapshot: ThreadSnapshot,
        val posts: List<BoardPost>,
        val checkpoint: PostId,
    ) : ThreadWatchResult

    data class Closed(
        val snapshot: ThreadSnapshot,
        val posts: List<BoardPost>,
        val checkpoint: PostId?,
    ) : ThreadWatchResult

    data object Deleted : ThreadWatchResult
}

class CheckWatchedThread(
    private val repository: ThreadRepository,
) {
    suspend operator fun invoke(subscription: ThreadWatchSubscription): DomainOutcome<ThreadWatchResult> =
        when (val outcome = repository.loadThread(subscription.address)) {
            is DomainOutcome.Success -> {
                DomainOutcome.Success(
                    outcome.value.evaluate(subscription.lastKnownPostId),
                )
            }

            is DomainOutcome.Failure -> {
                if (outcome.reason == DomainFailure.NotFound) {
                    DomainOutcome.Success(ThreadWatchResult.Deleted)
                } else {
                    DomainOutcome.Failure(outcome.reason)
                }
            }
        }
}

private fun ThreadSnapshot.evaluate(previousCheckpoint: PostId?): ThreadWatchResult {
    val latestPostId = posts.maxByOrNull { post -> post.address.post.value }?.address?.post
    val checkpoint =
        listOfNotNull(previousCheckpoint, latestPostId)
            .maxByOrNull(PostId::value)
    val newPosts =
        if (previousCheckpoint == null) {
            emptyList()
        } else {
            posts.filter { post -> post.address.post.value > previousCheckpoint.value }
        }
    return when {
        flags.closed || flags.archived -> {
            ThreadWatchResult.Closed(this, newPosts, checkpoint)
        }

        newPosts.isNotEmpty() && checkpoint != null -> {
            ThreadWatchResult.NewPosts(this, newPosts, checkpoint)
        }

        else -> {
            ThreadWatchResult.Unchanged(this, checkpoint)
        }
    }
}
