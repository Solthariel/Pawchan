package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import com.solthariel.pawchan.domain.posting.ReplyDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DvachNetworkPolicyTest {
    @Test
    fun catalogRequestsArePinnedToHttpsOrigin() {
        val request = CatalogRequestFactory.create(requireNotNull(BoardId.parse("b")))

        assertTrue(request.url.isHttps)
        assertEquals("2ch.su", request.url.host)
        assertEquals("/b/catalog.json", request.url.encodedPath)
        assertEquals("application/json", request.header("Accept"))
    }

    @Test
    fun threadRequestsArePinnedToHttpsOrigin() {
        val board =
            BoardAddress(
                site = requireNotNull(SiteId.parse("dvach")),
                board = requireNotNull(BoardId.parse("b")),
            )
        val address = ThreadAddress(board, requireNotNull(ThreadId.parse(42L)))

        val request = ThreadRequestFactory.create(address)

        assertTrue(request.url.isHttps)
        assertEquals("2ch.su", request.url.host)
        assertEquals("/b/res/42.json", request.url.encodedPath)
        assertEquals("application/json", request.header("Accept"))
    }

    @Test
    fun postingRequestsUsePinnedHttpsEndpoints() {
        val address = threadAddress()
        val captcha = PostingRequestFactory.captchaId(address.board, address.thread)
        val reply =
            PostingRequestFactory.reply(
                address,
                ReplyDraft(
                    comment = "Ответ",
                    emojiCaptchaToken = "posting-token",
                    attachments =
                        listOf(
                            ReplyAttachment(
                                fileName = "cat.png",
                                mimeType = "image/png",
                                content = byteArrayOf(1, 2, 3),
                            ),
                        ),
                ),
            )

        assertEquals("/api/captcha/emoji/id", captcha.url.encodedPath)
        assertEquals("2ch.su", captcha.url.host)
        assertEquals("b", captcha.url.queryParameter("board"))
        assertEquals("42", captcha.url.queryParameter("thread"))
        assertTrue(captcha.url.isHttps)
        assertEquals("/user/posting", reply.url.encodedPath)
        assertEquals("2ch.su", reply.url.host)
        assertEquals("POST", reply.method)
        val contentType = requireNotNull(requireNotNull(reply.body).contentType())
        assertTrue(contentType.toString().startsWith("multipart/form-data"))
    }

    @Test
    fun clientUsesFiniteTimeoutsAndDoesNotFollowRedirects() {
        val client = DvachHttpClient.create()

        assertEquals(CONNECT_TIMEOUT.toMillis().toInt(), client.connectTimeoutMillis)
        assertEquals(READ_TIMEOUT.toMillis().toInt(), client.readTimeoutMillis)
        assertEquals(CALL_TIMEOUT.toMillis().toInt(), client.callTimeoutMillis)
        assertFalse(client.followRedirects)
        assertFalse(client.followSslRedirects)
    }

    @Test
    fun contentClientUsesAnIndependentBoundedQueueAndSharesSessionContext() {
        val apiClient = DvachHttpClient.create()
        val contentClient = DvachHttpClient.createContentClient(apiClient)

        assertTrue(contentClient.followRedirects)
        assertFalse(contentClient.followSslRedirects)
        assertEquals(0, contentClient.callTimeoutMillis)
        assertFalse(contentClient.dispatcher === apiClient.dispatcher)
        assertEquals(CONTENT_REQUEST_LIMIT, contentClient.dispatcher.maxRequests)
        assertEquals(CONTENT_REQUEST_LIMIT, contentClient.dispatcher.maxRequestsPerHost)
        assertTrue(contentClient.connectionPool === apiClient.connectionPool)
        assertTrue(contentClient.cookieJar === apiClient.cookieJar)
    }

    private fun threadAddress(): ThreadAddress {
        val board =
            BoardAddress(
                site = requireNotNull(SiteId.parse("dvach")),
                board = requireNotNull(BoardId.parse("b")),
            )
        return ThreadAddress(board, requireNotNull(ThreadId.parse(42L)))
    }
}
