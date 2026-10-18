package com.solthariel.pawchan

import com.solthariel.pawchan.composer.allowedBy
import com.solthariel.pawchan.domain.posting.PostingIcon
import com.solthariel.pawchan.domain.posting.PostingIdentityRules
import com.solthariel.pawchan.domain.posting.PostingOptions
import com.solthariel.pawchan.domain.posting.PostingRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ComposerIconsPolicyTest {
    @Test
    fun onlyAnIconOfferedByCurrentBoardMayBeSubmitted() {
        val selected = PostingOptions(icon = 7)
        val reno = PostingRules(identity = PostingIdentityRules(icons = listOf(PostingIcon(7, "Рено"))))
        val other = PostingRules(identity = PostingIdentityRules(icons = listOf(PostingIcon(8, "Другой"))))
        assertEquals(7, selected.allowedBy(reno).icon)
        assertNull(selected.allowedBy(other).icon)
        assertNull(selected.allowedBy(PostingRules()).icon)
        assertNull(PostingOptions().allowedBy(reno).icon)
    }
}
