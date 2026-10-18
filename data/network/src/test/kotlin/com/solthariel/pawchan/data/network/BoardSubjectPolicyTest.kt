package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoardSubjectPolicyTest {
    private val address =
        ThreadAddress(
            BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("b"))),
            requireNotNull(ThreadId.parse(100)),
        )

    @Test fun disabledSubjectsKeepNavigationTitleButHidePostHeading() {
        val payload = """
{"board":{"enable_subject":false},
"threads":[{"posts":[{"num":100,"parent":0,"subject":"autotitle",
"comment":"body"}]}]}
"""
        val result = requireNotNull(ThreadPayloadDecoder(Json { ignoreUnknownKeys = true }).decode(address, payload))
        assertEquals("autotitle", result.subject)
        assertEquals(false, result.subjectsEnabled)
        assertNull(
            result.posts
                .single()
                .content.metadata.subject,
        )
        assertEquals(
            "body",
            result.posts
                .single()
                .content.document.plainText,
        )
    }

    @Test fun enabledSubjectsPreserveUserHeading() {
        val payload = """
{"board":{"enable_subject":true},
"threads":[{"posts":[{"num":100,"parent":0,"subject":"heading",
"comment":"body"}]}]}
"""
        val result = requireNotNull(ThreadPayloadDecoder(Json { ignoreUnknownKeys = true }).decode(address, payload))
        assertEquals(
            "heading",
            result.posts
                .single()
                .content.metadata.subject,
        )
    }

    @Test fun referencePreservesBoardAndThread() {
        val document = PostDocumentParser.parse("""<a href="/tech/res/200.html#201">&gt;&gt;201</a>""")
        val reference = document.runs.single().link as PostLink.Reference
        assertEquals(201L, reference.post.value)
        assertEquals(
            "tech",
            reference.thread
                ?.board
                ?.board
                ?.value,
        )
        assertEquals(200L, reference.thread?.thread?.value)
    }
}
