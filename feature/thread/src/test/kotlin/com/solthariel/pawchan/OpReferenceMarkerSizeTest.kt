package com.solthariel.pawchan

import com.solthariel.pawchan.thread.LINK_SUFFIX_LOCAL_OP
import com.solthariel.pawchan.thread.linkSuffixLabel
import org.junit.Assert.assertEquals
import org.junit.Test

class OpReferenceMarkerSizeTest {
    @Test fun localOpeningPostHasOpTextLabel() {
        assertEquals("OP", linkSuffixLabel(LINK_SUFFIX_LOCAL_OP, null))
    }
}
