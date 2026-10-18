package com.solthariel.pawchan.thread.watch

import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.thread.ThreadRepository
import com.solthariel.pawchan.thread.toThreadError

internal suspend fun loadAutomaticUpdate(
    repository: ThreadRepository,
    address: ThreadAddress,
    previous: PostId?,
): ForegroundThreadWatchUpdate =
    when (val result = repository.loadThread(address)) {
        is DomainOutcome.Success -> {
            ForegroundThreadWatchUpdate(
                result.value,
                ThreadWatcherPoll(
                    newPosts = result.value.posts.count { previous != null && it.address.post.value > previous.value },
                    stopReason =
                        ThreadWatcherStopReason.CLOSED.takeIf {
                            result.value.flags.closed || result.value.flags.archived
                        },
                ),
            )
        }

        is DomainOutcome.Failure -> {
            ForegroundThreadWatchUpdate(null, ThreadWatcherPoll(0, result.reason.toThreadError()))
        }
    }
