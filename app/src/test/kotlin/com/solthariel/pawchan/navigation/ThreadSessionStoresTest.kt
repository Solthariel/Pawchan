package com.solthariel.pawchan.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Повторный вход в тред получает ту же сессию; хранилище ограничено и закрывает освобождённые. */
class ThreadSessionStoresTest {
    @Test
    fun reenteringThreadReturnsTheSameSession() {
        val stores = ThreadSessionStores()
        val address = defaultThreadAddress(1)
        val first = session(stores, address)
        val second = session(stores, address)
        assertSame(first, second)
        assertFalse(first.cleared)
    }

    @Test
    fun oldestSessionBeyondLimitIsCleared() {
        val stores = ThreadSessionStores()
        val sessions = (1..MAX_RETAINED_THREADS + 1).map { session(stores, defaultThreadAddress(it.toLong())) }
        assertTrue(sessions.first().cleared)
        assertTrue(sessions.drop(1).none { it.cleared })
        assertEquals(MAX_RETAINED_THREADS, stores.size)
        assertNotSame(sessions.first(), session(stores, defaultThreadAddress(1)))
    }

    @Test
    fun closedThreadAndActivityEndReleaseSessions() {
        val owner = ViewModelStore()
        val factory = viewModelFactory { initializer { ThreadSessionStores() } }
        val stores = ViewModelProvider.create(owner, factory)[ThreadSessionStores::class]
        val closed = session(stores, defaultThreadAddress(1))
        val kept = session(stores, defaultThreadAddress(2))
        stores.drop(listOf(defaultThreadAddress(1)))
        assertTrue(closed.cleared)
        assertFalse(kept.cleared)
        owner.clear()
        assertTrue(kept.cleared)
        assertEquals(0, stores.size)
    }

    private fun session(
        stores: ThreadSessionStores,
        address: com.solthariel.pawchan.domain.model.ThreadAddress,
    ): Probe {
        val factory = viewModelFactory { initializer { Probe() } }
        return ViewModelProvider.create(stores.owner(address), factory)[Probe::class]
    }

    private class Probe : ViewModel() {
        var cleared = false

        override fun onCleared() {
            cleared = true
        }
    }
}
