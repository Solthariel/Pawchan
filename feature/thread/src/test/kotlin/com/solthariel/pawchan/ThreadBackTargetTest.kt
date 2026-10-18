package com.solthariel.pawchan

import com.solthariel.pawchan.thread.ThreadBackTarget
import com.solthariel.pawchan.thread.threadBackTarget
import org.junit.Assert.assertEquals
import org.junit.Test

class ThreadBackTargetTest {
    @Test fun viewerClosesBeforeGallerySelectionOrSearch() {
        assertEquals(ThreadBackTarget.MEDIA, threadBackTarget(true, true, true, true))
    }

    @Test fun returnedGalleryClosesBeforeRoute() {
        assertEquals(ThreadBackTarget.DIALOG, threadBackTarget(false, true, false, false))
    }

    @Test fun directPostMediaReturnsToThreadNotGallery() {
        assertEquals(ThreadBackTarget.MEDIA, threadBackTarget(true, false, false, false))
        assertEquals(ThreadBackTarget.ROUTE, threadBackTarget(false, false, false, false))
    }

    @Test fun selectionAndSearchKeepTheirBackPriority() {
        assertEquals(ThreadBackTarget.SELECTION, threadBackTarget(false, false, true, true))
        assertEquals(ThreadBackTarget.SEARCH, threadBackTarget(false, false, false, true))
    }
}
