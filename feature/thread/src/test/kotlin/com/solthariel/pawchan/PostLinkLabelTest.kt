package com.solthariel.pawchan

import com.solthariel.pawchan.thread.PostLinkTarget
import com.solthariel.pawchan.thread.openLabel
import org.junit.Assert.assertEquals
import org.junit.Test

class PostLinkLabelTest {
    private val thread = defaultThreadAddress(100)

    @Test fun postAnchorOpensPostAndThreadOrOpOpensThread() {
        assertEquals("Открыть пост", PostLinkTarget("u", thread, postId = 105).openLabel(thread))
        assertEquals("Открыть тред", PostLinkTarget("u", thread, postId = 100).openLabel(thread))
        assertEquals("Открыть тред", PostLinkTarget("u", thread).openLabel(thread))
    }
}
