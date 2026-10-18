package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.RemoteUrl
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.infrastructure.sites.DvachWebLinks
import com.solthariel.pawchan.infrastructure.sites.ForumWebLinks
import com.solthariel.pawchan.infrastructure.thread.AppPostLinkResolver
import com.solthariel.pawchan.navigation.defaultThreadAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

class PostLinkSiteBoundaryTest {
    @Test fun unknownSiteNeverFallsBackToDvach() {
        val resolver = AppPostLinkResolver(listOf(DvachWebLinks()))
        val address = otherAddress()
        assertNull(resolver.postUrl(address, 7))
        assertNull(
            resolver.resolve(
                PostLink.Reference(requireNotNull(PostId.parse(7)), address),
                defaultThreadAddress(42),
            ),
        )
    }

    @Test fun twoRegisteredSitesKeepCollidingNumbersAndCrossReferencesIndependent() {
        val resolver = AppPostLinkResolver(listOf(DvachWebLinks(), FakeForumLinks()))
        assertEquals("https://2ch.su/b/res/42.html#7", resolver.postUrl(defaultThreadAddress(42), 7))
        val target =
            resolver.resolve(
                PostLink.Reference(requireNotNull(PostId.parse(7)), otherAddress()),
                defaultThreadAddress(42),
            )
        assertEquals("https://forum.example.test/topics/42?comment=7", target?.url)
        assertEquals(otherAddress(), target?.thread)
    }

    @Test fun exactMirrorsRemainOneSiteAndLookalikesOrOtherPortsAreNotForumTargets() {
        val resolver = AppPostLinkResolver(listOf(DvachWebLinks()))
        assertEquals(defaultThreadAddress(42), external(resolver, "https://2ch.life/b/res/42.html")?.thread)
        assertEquals(defaultThreadAddress(42), external(resolver, "https://2ch.hk:443/b/res/42.html")?.thread)
        assertNull(external(resolver, "https://2ch.hk:444/b/res/42.html")?.thread)
        assertNull(external(resolver, "https://2ch.hk.evil.test/b/res/42.html")?.thread)
        assertNull(external(resolver, "https://u:p@2ch.hk/b/res/42.html"))
    }

    @Test fun invalidPostIdsDoNotProduceSyntheticLinks() {
        val resolver = AppPostLinkResolver(listOf(DvachWebLinks()))
        assertNull(resolver.postUrl(defaultThreadAddress(42), 0))
        assertNull(resolver.postUrl(defaultThreadAddress(42), -7))
    }

    @Test fun duplicateRegistrationIsRejected() {
        var rejected = false
        try {
            AppPostLinkResolver(listOf(DvachWebLinks(), DvachWebLinks()))
        } catch (_: IllegalStateException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test fun registryCopiesTheRegistrationListInsteadOfBorrowingMutableMembership() {
        val adapters = mutableListOf<ForumWebLinks>(DvachWebLinks())
        val resolver = AppPostLinkResolver(adapters)
        adapters.clear()
        assertEquals("https://2ch.su/b/res/42.html#7", resolver.postUrl(defaultThreadAddress(42), 7))
    }

    @Test fun anAdapterCannotClassifyAnotherSitesAddressAsItsOwn() {
        val bad =
            object : ForumWebLinks {
                override val site = otherAddress().board.site

                override fun threadUrl(
                    address: ThreadAddress,
                    postId: Long?,
                ): String? = null

                override fun threadAddress(uri: URI): ThreadAddress = defaultThreadAddress(42)
            }
        assertNull(external(AppPostLinkResolver(listOf(bad)), "https://forum.example.test/topics/42")?.thread)
    }

    private fun external(
        resolver: AppPostLinkResolver,
        url: String,
    ) = resolver.resolve(PostLink.External(requireNotNull(RemoteUrl.parse(url))), null)
}

private fun otherAddress(): ThreadAddress =
    ThreadAddress(
        BoardAddress(requireNotNull(SiteId.parse("test-forum")), requireNotNull(BoardId.parse("b"))),
        requireNotNull(ThreadId.parse(42)),
    )

private class FakeForumLinks : ForumWebLinks {
    override val site = otherAddress().board.site

    override fun threadUrl(
        address: ThreadAddress,
        postId: Long?,
    ): String? =
        if (address.board.site == site) {
            "https://forum.example.test/topics/${address.thread.value}?comment=$postId"
        } else {
            null
        }

    override fun threadAddress(uri: URI): ThreadAddress? = null
}
