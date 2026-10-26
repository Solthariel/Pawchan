package com.solthariel.pawchan

import com.solthariel.pawchan.thread.PostPreviewTarget
import com.solthariel.pawchan.thread.PreviewStackPath
import com.solthariel.pawchan.thread.RETAINED_PREVIEW_WINDOWS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PreviewStackPathTest {
    @Test fun pushAndPopPreserveParentIdentityIncludingRepeatedPosts() {
        val path = PreviewStackPath()
        val first = PostPreviewTarget(defaultThreadAddress(1), 2)
        val second = PostPreviewTarget(defaultThreadAddress(1), 3)
        val initial = path.reconcile(listOf(first)).single()
        val nested = path.reconcile(listOf(first, second, first))
        assertEquals(initial, nested.first())
        assertNotEquals(nested.first().id, nested.last().id)
        assertEquals(initial, path.reconcile(listOf(first)).single())
    }

    @Test fun differentParentChainsNeverShareSavedState() {
        val path = PreviewStackPath()
        val first = PostPreviewTarget(defaultThreadAddress(1), 2)
        val second = PostPreviewTarget(defaultThreadAddress(1), 3)
        val before = path.reconcile(listOf(first, second)).last().id
        val after = path.reconcile(listOf(second, second)).last().id
        assertNotEquals(before, after)
    }

    @Test fun keysSurviveRecreationAndOnlyFiveWindowsAreLive() {
        val targets = (1L..20L).map { PostPreviewTarget(defaultThreadAddress(1), it) }
        val entries = PreviewStackPath().reconcile(targets)
        assertEquals(entries, PreviewStackPath().reconcile(targets))
        assertEquals(5, entries.takeLast(RETAINED_PREVIEW_WINDOWS).size)
    }
}
