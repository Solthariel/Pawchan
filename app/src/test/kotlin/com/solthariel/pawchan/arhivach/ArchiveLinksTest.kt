package com.solthariel.pawchan.arhivach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArchiveLinksTest {
    @Test
    fun linksUseArchiveAndPostIdentifiers() {
        assertEquals("https://arhivach.vc/thread/1461983/", archiveThreadLink(1461983))
        assertEquals("https://arhivach.vc/thread/1461983/#19985932", archivePostLink(1461983, 19985932))
        assertNull(archiveThreadLink(null))
        assertNull(archiveThreadLink(0))
        assertNull(archivePostLink(1461983, 0))
    }
}
