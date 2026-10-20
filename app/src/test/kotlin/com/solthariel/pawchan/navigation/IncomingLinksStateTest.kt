package com.solthariel.pawchan.navigation

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IncomingLinksStateTest {
    private val first = "https://2ch.su/b/res/123.html#456"
    private val second = "https://2ch.org/mobi/res/789.html#800"

    @Test fun newestUrlStaysPendingUntilNavigationAcknowledgesIt() {
        val state = IncomingLinksViewModel(SavedStateHandle())
        state.accept("android.intent.action.VIEW", first)
        assertEquals(first, state.pending.value)
        state.accept("android.intent.action.VIEW", second)
        state.consume(first)
        assertEquals(second, state.pending.value)
        state.consume(second)
        assertNull(state.pending.value)
    }

    @Test fun invalidIntentsCannotOverwriteAnAcceptedLink() {
        val state = IncomingLinksViewModel(SavedStateHandle())
        state.accept("android.intent.action.VIEW", first)
        state.accept("android.intent.action.MAIN", second)
        state.accept("android.intent.action.VIEW", "https://evil.example/b/")
        assertEquals(first, state.pending.value)
    }

    @Test fun identicalLinkCanBeOpenedAgainAfterConsumption() {
        val state = IncomingLinksViewModel(SavedStateHandle())
        state.accept("android.intent.action.VIEW", first)
        state.consume(first)
        state.accept("android.intent.action.VIEW", first)
        assertEquals(first, state.pending.value)
    }

    @Test fun pendingLinkIsBackedBySavedState() {
        val saved = SavedStateHandle()
        IncomingLinksViewModel(saved).accept("android.intent.action.VIEW", first)
        assertEquals(first, IncomingLinksViewModel(saved).pending.value)
    }
}
