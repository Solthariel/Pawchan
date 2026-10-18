package com.solthariel.pawchan

import com.solthariel.pawchan.core.ui.formatByteSizeForUi
import com.solthariel.pawchan.thread.ThreadContentFlags
import com.solthariel.pawchan.thread.ThreadContentPresentation
import com.solthariel.pawchan.thread.ThreadContentStatistics
import com.solthariel.pawchan.thread.ThreadUiState
import com.solthariel.pawchan.thread.threadMenuStatistics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ThreadMediaSizeLabelsTest {
    @Test
    fun threadMenuHidesInequalityWithoutInventingMissingBytes() {
        val content =
            ThreadUiState.Content(
                id = 1L,
                subject = null,
                posts = emptyList(),
                flags = ThreadContentFlags(false),
                presentation = ThreadContentPresentation(false),
                statistics = ThreadContentStatistics(mediaBytes = 1024L, mediaBytesComplete = false),
            )
        assertEquals(formatByteSizeForUi(1024L), threadMenuStatistics(content).size)
        assertFalse(content.mediaBytesComplete)
    }
}
