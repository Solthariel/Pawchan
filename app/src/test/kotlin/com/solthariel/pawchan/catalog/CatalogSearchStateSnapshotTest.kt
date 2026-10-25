package com.solthariel.pawchan.catalog

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogSearchStateSnapshotTest {
    @Test fun queryParticipatesInSnapshotObservation() {
        val state = CatalogSearchState(mutableStateOf(false), TestFocusManager(), null)
        val reads = mutableListOf<Any>()
        Snapshot.observe(readObserver = { reads += it }) { assertEquals("", state.query) }
        assertTrue(
            "Query changes must invalidate consumers that read the same search-state instance",
            reads.isNotEmpty(),
        )
    }

    @Test fun editAndCloseKeepTheParentQueryAndVisibilityConsistent() {
        val focus = TestFocusManager()
        val state = CatalogSearchState(mutableStateOf(false), focus, null)
        state.onQueryChange = { state.query = it }
        state.open()
        state.edit("хлеб")
        assertEquals("хлеб", state.query)
        assertTrue(state.visible)
        state.close()
        assertEquals("", state.query)
        assertFalse(state.visible)
        assertFalse(state.focusRequested)
        assertTrue(focus.cleared)
    }
}

private class TestFocusManager : FocusManager {
    var cleared = false
        private set

    override fun clearFocus(force: Boolean) {
        cleared = true
    }

    override fun moveFocus(focusDirection: FocusDirection): Boolean = false
}
