package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.posting.PostingOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PostingRulesDecoderTest {
    private val board = BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("b")))

    @Test
    fun readsBooleanFlagsKilobyteLimitAndSupportedExtensions() {
        val result =
            decodePostingRules(
                board,
                """{"board":{"id":"b","enable_subject":false,
            "max_files_size":40960,"file_types":["jpg","png","mp4"],"max_comment":500}}""",
            )
                as PostingOutcome.Success
        assertFalse(result.value.subjectEnabled)
        assertEquals(4, result.value.maxFiles)
        assertEquals(40 * 1024 * 1024, result.value.maxFileBytes)
        assertEquals(500, result.value.maxCommentLength)
        assertTrue("jpeg" in result.value.extensions)
    }

    @Test
    fun rejectsWrongBoardAndUsesDocumentedFourFileDefault() {
        assertTrue(decodePostingRules(board, """{"board":{"id":"biz"}}""") is PostingOutcome.Failure)
        val result =
            decodePostingRules(board, """{"board":{"id":"b","enable_subject":0}}""")
                as PostingOutcome.Success
        assertFalse(result.value.subjectEnabled)
        assertEquals(4, result.value.maxFiles)
    }

    @Test
    fun readsNameSageAndEightFileRulesFromSelectedBoard() {
        val outcome =
            decodePostingRules(
                board,
                """{"board":{"id":"b","enable_names":0,"enable_sage":true,"max_files_count":8}}""",
            )
                as PostingOutcome.Success
        assertFalse(outcome.value.namesEnabled)
        assertTrue(outcome.value.sageEnabled)
        assertEquals(8, outcome.value.maxFiles)
        val enabled =
            decodePostingRules(board, """{"board":{"id":"b","enable_names":true,"enable_sage":0}}""")
                as PostingOutcome.Success
        assertTrue(enabled.value.namesEnabled)
        assertFalse(enabled.value.sageEnabled)
    }

    @Test
    fun detectsTagsAvailability() {
        val outcome =
            decodePostingRules(
                board,
                """{"board":{"id":"b","tags":[],"enable_oekaki":true}}""",
            ) as PostingOutcome.Success
        assertTrue(outcome.value.tagsEnabled)
    }
}
