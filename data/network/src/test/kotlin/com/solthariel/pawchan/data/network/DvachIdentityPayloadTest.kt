package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DvachIdentityPayloadTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun threadDecodesHeavenWithoutChangingSubjectOrComment() {
        val address =
            ThreadAddress(
                BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("test"))),
                requireNotNull(ThreadId.parse(252013)),
            )
        val payload = """{"threads":[{"posts":[
            {"num":252013,"parent":0,"op":1,"name":"test","comment":"root"},
            {"num":252473,"parent":252013,"name":"Аноним&nbsp;ID:&nbsp;Heaven","email":"",
             "subject":"ID: Heaven","comment":"Heaven OP sage"}]}]}"""
        val posts = requireNotNull(ThreadPayloadDecoder(json).decode(address, payload)).posts
        val reply = posts.last().content
        assertEquals("Аноним", reply.metadata.name)
        assertTrue(reply.metadata.sage)
        assertEquals("ID: Heaven", reply.metadata.subject)
        assertEquals("Heaven OP sage", reply.document.plainText)
        assertTrue(
            posts
                .first()
                .content.metadata.originalPoster,
        )
    }

    @Test fun catalogUsesTheSameWireIdentityMapping() {
        val payload = """{"board":{"id":"test","name":"test"},"threads":[
            {"num":252013,"name":"Аноним&nbsp;ID:&nbsp;Heaven","email":"","comment":"OP sage"}]}"""
        val metadata =
            requireNotNull(CatalogPayloadDecoder(json).decode(payload))
                .threads
                .single()
                .content.metadata
        assertEquals("Аноним", metadata.name)
        assertTrue(metadata.sage)
    }
}
