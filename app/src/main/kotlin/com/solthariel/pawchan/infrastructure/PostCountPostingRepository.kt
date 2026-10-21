package com.solthariel.pawchan.infrastructure

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.posting.NewThreadDraft
import com.solthariel.pawchan.domain.posting.PostingFailure
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.posting.PostingProgressObserver
import com.solthariel.pawchan.domain.posting.PostingRepository
import com.solthariel.pawchan.domain.posting.ReplyDraft
import com.solthariel.pawchan.domain.settings.AppSettingsRepository
import com.solthariel.pawchan.domain.statistics.UsageStatisticsRepository
import com.solthariel.pawchan.domain.statistics.appendPostCount
import com.solthariel.pawchan.domain.statistics.hasPostCountCommand
import kotlinx.coroutines.flow.first

/** Expansion happens only on Send; retries start with the unmodified draft. */
internal class PostCountPostingRepository(
    private val delegate: PostingRepository,
    private val settings: AppSettingsRepository,
    private val stats: UsageStatisticsRepository,
) : PostingRepository by delegate {
    override suspend fun submitReply(
        address: ThreadAddress,
        draft: ReplyDraft,
        observer: PostingProgressObserver,
    ): PostingOutcome<PostId> =
        when (val comment = expand(address.board, draft.comment)) {
            is PostingOutcome.Success -> delegate.submitReply(address, draft.copy(comment = comment.value), observer)
            is PostingOutcome.Failure -> comment
        }

    override suspend fun createThread(
        board: BoardAddress,
        draft: NewThreadDraft,
        observer: PostingProgressObserver,
    ): PostingOutcome<ThreadAddress> =
        when (val comment = expand(board, draft.comment)) {
            is PostingOutcome.Success -> delegate.createThread(board, draft.copy(comment = comment.value), observer)
            is PostingOutcome.Failure -> comment
        }

    private suspend fun expand(
        board: BoardAddress,
        comment: String,
    ): PostingOutcome<String> {
        if (!hasPostCountCommand(comment) || !settings.appSettings.first().postCountEnabled) {
            return PostingOutcome.Success(comment)
        }
        val days = stats.days.first()
        return if (!stats.available.first()) {
            rejected("Не удалось прочитать статистику для /postcount. Пост не отправлен.")
        } else {
            validateExpanded(board, appendPostCount(comment, days))
        }
    }

    private suspend fun validateExpanded(
        board: BoardAddress,
        expanded: String,
    ): PostingOutcome<String> =
        when (val result = delegate.loadRules(board)) {
            is PostingOutcome.Failure -> {
                result
            }

            is PostingOutcome.Success -> {
                val limit = minOf(result.value.maxCommentLength, ReplyDraft.MAX_COMMENT_LENGTH)
                if (expanded.length > limit) {
                    rejected("Пост со статистикой слишком длинный. Сократите текст или удалите /postcount.")
                } else {
                    PostingOutcome.Success(expanded)
                }
            }
        }
}

private fun rejected(reason: String) = PostingOutcome.Failure(PostingFailure.Rejected(reason))
