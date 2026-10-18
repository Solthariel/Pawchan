package com.solthariel.pawchan.thread

import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.posting.PostingFailure
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.thread.ReportingThreadRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class ThreadReportState(
    val sending: Boolean = false,
    val sent: Int = 0,
    val failure: String? = null,
)

internal class ThreadReportController(
    private val scope: CoroutineScope,
    private val address: ThreadAddress,
    private val repository: ReportingThreadRepository?,
) {
    private val mutableState = MutableStateFlow(ThreadReportState())
    private var job: Job? = null
    val state = mutableState.asStateFlow()
    val available: Boolean get() = repository != null

    fun submit(
        ids: List<Long>,
        comment: String,
    ) {
        val target = repository ?: return
        if (job?.isActive == true || comment.isBlank() || ids.isEmpty()) return
        job =
            scope.launch {
                mutableState.value = ThreadReportState(sending = true)
                try {
                    for (post in ids.distinct().mapNotNull(PostId::parse)) {
                        when (val result = target.reportPost(address, post, comment)) {
                            is PostingOutcome.Success -> {
                                mutableState.value = mutableState.value.copy(sent = mutableState.value.sent + 1)
                            }

                            is PostingOutcome.Failure -> {
                                val reason = (result.reason as? PostingFailure.Rejected)?.reason
                                mutableState.value =
                                    mutableState.value.copy(failure = reason ?: "Не удалось отправить жалобу")
                                break
                            }
                        }
                    }
                } finally {
                    mutableState.value = mutableState.value.copy(sending = false)
                }
            }
    }

    fun cancel() {
        job?.cancel()
    }

    fun reset() {
        if (job?.isActive != true) mutableState.value = ThreadReportState()
    }
}
