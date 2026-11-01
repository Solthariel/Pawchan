package com.solthariel.pawchan.thread

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThreadWriteActionTest {
    @Test fun openThreadRequestsAnEmptyReplyInTheCurrentThread() {
        val requests = mutableListOf<ThreadReplyRequest>()
        requireNotNull(threadWriteAction(content(false), requests::add)).invoke()
        assertEquals(listOf(ThreadReplyRequest(false, null)), requests)
    }

    @Test fun closedLoadingAndErrorHaveNoWriteAction() {
        var calls = 0
        listOf(ThreadUiState.Loading, ThreadUiState.Error(ThreadError.NETWORK), content(true)).forEach { state ->
            assertNull(threadWriteAction(state) { calls++ })
        }
        assertEquals(0, calls)
    }

    private fun content(closed: Boolean) =
        ThreadUiState.Content(
            42,
            null,
            emptyList(),
            ThreadContentFlags(closed),
            ThreadContentPresentation(false),
            ThreadContentStatistics(),
        )
}
