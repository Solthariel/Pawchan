package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoardDetailsPayloadDecoderTest {
    private val decoder = BoardDetailsPayloadDecoder(Json)
    private val board = BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("biz")))

    @Test fun archivePreservesOnlyRealMetadataWithoutInventingMedia() {
        val board =
            com.solthariel.pawchan.domain.model.BoardAddress(
                com.solthariel.pawchan.domain.model.SiteId
                    .parse("dvach")!!,
                com.solthariel.pawchan.domain.model.BoardId
                    .parse("mobi")!!,
            )
        val decoder = BoardDetailsPayloadDecoder(dvachReadJson())
        val result =
            decoder.archive(
                board,
                """{"threads":[{"thread":42,"subject":"Тема &amp; текст",""" +
                    """"timestamp":1566572622,"date":"2020-05-09"}],"pages":[]}""",
            )!!
        val row = result.threads.single()
        org.junit.Assert.assertEquals("Тема & текст", row.subject)
        org.junit.Assert.assertEquals(1566572622L, row.timestampSeconds)
        org.junit.Assert.assertEquals("2020-05-09", row.archiveDate)
        val absent = decoder.archive(board, """{"threads":[{"thread":43}],"pages":[]}""")!!.threads.single()
        org.junit.Assert.assertNull(absent.timestampSeconds)
        org.junit.Assert.assertNull(absent.archiveDate)
        org.junit.Assert.assertEquals("Без темы", absent.subject)
    }

    @Test
    fun archiveParsesStringAndNumericIdsAndDeduplicates() {
        val result =
            requireNotNull(
                decoder.archive(
                    board,
                    """{"threads":[
            {"thread":"42","subject":"<b>A &amp; B</b>"},
            {"thread":43,"subject":"Нет темы"},{"thread":42,"subject":"duplicate"}],"pages":[0,1,2]}""",
                ),
            )
        assertEquals(listOf(42L, 43L), result.threads.map { it.address.thread.value })
        assertEquals(listOf("A & B", "Без темы"), result.threads.map { it.subject })
        assertEquals(listOf(0, 1, 2), result.pages)
    }

    @Test
    fun malformedRowsDoNotDiscardValidArchiveEntries() {
        val result =
            requireNotNull(
                decoder.archive(
                    board,
                    """{"threads":[{},false,
            {"thread":0},{"thread":7}],"pages":[-1,"bad",2,2]}""",
                ),
            )
        assertEquals(listOf(7L), result.threads.map { it.address.thread.value })
        assertEquals(listOf(2), result.pages)
    }

    @Test
    fun emptyArchiveIsValidButMissingThreadsIsNot() {
        assertEquals(emptyList<Any>(), requireNotNull(decoder.archive(board, """{"threads":[]}""")).threads)
        assertNull(decoder.archive(board, "{}"))
        assertNull(decoder.archive(board, "[]"))
    }

    @Test
    fun informationUsesBoardMetadataAndClearsMarkupOffUi() {
        val result =
            requireNotNull(
                decoder.information(
                    board,
                    """{"board":{"id":"biz","name":"Бизнес",
            "info":"<p>Правила &amp; FAQ</p>","default_name":"Аноним","bump_limit":500,"max_comment":15000}}""",
                ),
            )
        assertEquals("Бизнес", result.title)
        assertEquals("Правила & FAQ", result.description.trim())
        assertEquals(500, result.bumpLimit)
        assertEquals(15000, result.maxCommentLength)
    }

    @Test
    fun readsConfigurationFlagsWithoutInventingMissingValues() {
        val info =
            requireNotNull(
                decoder.information(
                    board,
                    """{"board":{"id":"biz","name":"Бизнес","enable_subject":false,"enable_names":0}}""",
                ),
            )
        assertEquals(false, info.configuration.subjectsEnabled)
        assertEquals(false, info.configuration.namesEnabled)
        val unknown = requireNotNull(decoder.information(board, """{"board":{"id":"biz","name":"Бизнес"}}"""))
        assertNull(unknown.configuration.subjectsEnabled)
        assertNull(unknown.configuration.namesEnabled)
    }

    @Test
    fun wrongBoardOrMissingMetadataIsRejected() {
        assertNull(decoder.information(board, """{"board":{"id":"b","name":"Бред"}}"""))
        assertNull(decoder.information(board, "{}"))
    }

    @Test
    fun missingAndInvalidOptionalLimitsStayUnknown() {
        val result =
            requireNotNull(
                decoder.information(
                    board,
                    """{"board":{"id":"biz","name":"Бизнес","bump_limit":0,"max_comment":"bad"}}""",
                ),
            )
        assertNull(result.bumpLimit)
        assertNull(result.maxCommentLength)
        assertEquals("", result.description)
    }
}
