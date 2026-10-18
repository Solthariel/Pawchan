package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.navigation.defaultBoardAddress
import com.solthariel.pawchan.navigation.publicThreadLink
import org.junit.Assert.assertEquals
import org.junit.Test

class PublicThreadLinksTest {
    @Test
    fun threadAndPostUseTheSamePublicOriginAndPath() {
        val address = ThreadAddress(defaultBoardAddress(), requireNotNull(ThreadId.parse(42)))
        assertEquals("https://2ch.su/b/res/42.html", publicThreadLink(address))
        assertEquals("https://2ch.su/b/res/42.html#43", publicThreadLink(address, 43))
        assertEquals("https://2ch.org/b/res/42.html", publicThreadLink(address, mirror = "2ch.org"))
    }

    @Test fun unsupportedSiteDoesNotProduceDvachLink() {
        val board =
            defaultBoardAddress().copy(
                site =
                    requireNotNull(
                        com.solthariel.pawchan.domain.model.SiteId
                            .parse("other"),
                    ),
            )
        assertEquals(null, publicThreadLink(ThreadAddress(board, requireNotNull(ThreadId.parse(42)))))
    }
}
