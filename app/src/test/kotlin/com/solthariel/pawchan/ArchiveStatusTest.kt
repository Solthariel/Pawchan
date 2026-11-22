package com.solthariel.pawchan

import com.solthariel.pawchan.arhivach.toArchiveStatus
import com.solthariel.pawchan.core.ui.PostStateMarker
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.threads.ThreadListStatusTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArchiveStatusTest {
    @Test
    fun statusKeepsListIconsAndUncertainDeletion() {
        val expected =
            listOf(
                Triple("активный", PostStateMarker.ARCHIVE_ACTIVE, ThreadListStatusTone.PRIMARY),
                Triple("Удалён?", PostStateMarker.ARCHIVE_MAYBE_DELETED, ThreadListStatusTone.TERTIARY),
                Triple("saved", PostStateMarker.ARCHIVE_SAVED, ThreadListStatusTone.SECONDARY),
                Triple("потерян", PostStateMarker.ARCHIVE_LOST, ThreadListStatusTone.ERROR),
                Triple("queue", PostStateMarker.ARCHIVE_QUEUED, ThreadListStatusTone.MUTED),
            )
        expected.forEach { (raw, marker, tone) ->
            val status = raw.toArchiveStatus()!!
            assertEquals(marker, status.postMarker)
            assertEquals(marker.icon, status.listMarker.icon)
            assertEquals(tone, status.listMarker.tone)
        }
        assertEquals(
            "Архивач: исходный тред, возможно, удалён",
            "deleted?".toArchiveStatus()?.listMarker?.description,
        )
    }

    @Test
    fun unknownStatusPreservesTextAndMissingStatusStaysAbsent() {
        val unknown = "  Новый статус  ".toArchiveStatus()!!
        assertEquals(PostStateMarker.ARCHIVE_OTHER, unknown.postMarker)
        assertEquals(UiIconKind.INFO, unknown.listMarker.icon)
        assertEquals("Статус Архивача: Новый статус", unknown.listMarker.description)
        assertNull((null as String?).toArchiveStatus())
        assertNull("  ".toArchiveStatus())
    }
}
