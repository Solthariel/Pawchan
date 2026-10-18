package com.solthariel.pawchan.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IdentifiersTest {
    @Test
    fun textualIdentifiersAreNormalizedAndValidated() {
        assertEquals("dvach", SiteId.parse("  DVACH  ")?.value)
        assertEquals("b", BoardId.parse("B")?.value)
        assertNull(SiteId.parse(""))
        assertNull(BoardId.parse("/b/"))
        assertNull(BoardId.parse("two words"))
    }

    @Test
    fun numericIdentifiersMustBePositive() {
        assertEquals(42L, ThreadId.parse(42L)?.value)
        assertEquals(7L, PostId.parse(7L)?.value)
        assertNull(ThreadId.parse(0L))
        assertNull(PostId.parse(-1L))
    }

    @Test
    fun remoteUrlsRequireEncryptedTransport() {
        assertEquals("https://2ch.su/b/", RemoteUrl.parse(" https://2ch.su/b/ ")?.value)
        assertNull(RemoteUrl.parse("http://2ch.su/b/"))
        assertNull(RemoteUrl.parse("/b/catalog.json"))
    }
}
