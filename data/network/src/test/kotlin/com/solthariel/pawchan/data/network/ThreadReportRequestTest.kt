package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.posting.PostingOutcome
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadReportRequestTest {
    @Test fun requestUsesConfirmedMultipartFields() {
        val address =
            ThreadAddress(
                BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("b"))),
                requireNotNull(ThreadId.parse(100)),
            )
        val request = reportPostRequest(address, requireNotNull(PostId.parse(101)), "Причина")
        val body = Buffer().also { requireNotNull(request.body).writeTo(it) }.readUtf8()
        assertEquals("https://2ch.su/user/report", request.url.toString())
        assertEquals("POST", request.method)
        for (name in listOf("board", "thread", "post", "comment")) assertTrue(body.contains("name=\"$name\""))
        assertTrue(body.contains("101"))
    }

    @Test fun responseRequiresExplicitSuccess() {
        assertEquals(PostingOutcome.Success(Unit), decodeReportResponse("{\"result\":\"1\"}"))
        assertTrue(decodeReportResponse("{\"result\":\"0\",\"message\":\"Отклонено\"}") is PostingOutcome.Failure)
        assertTrue(decodeReportResponse("{}") is PostingOutcome.Failure)
    }
}
