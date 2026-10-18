package com.solthariel.pawchan.data.maintenance

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class MaintenanceGateTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun independentInstancesSerializeMutations() =
        runBlocking {
            val first = MaintenanceGate(temporary.root)
            val second = MaintenanceGate(temporary.root)
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val owner =
                launch {
                    first.access {
                        entered.complete(Unit)
                        release.await()
                    }
                }
            entered.await()
            val waiting = async { second.access { "next" } }
            delay(50)
            assertFalse(waiting.isCompleted)
            release.complete(Unit)
            owner.join()
            assertEquals("next", waiting.await())
        }

    @Test fun nestedOperationsAcrossDispatchersDoNotDeadlock() =
        runBlocking {
            val first = MaintenanceGate(temporary.root)
            val second = MaintenanceGate(temporary.root)
            val result = first.access { withContext(Dispatchers.Default) { second.access { "nested" } } }
            assertEquals("nested", result)
        }

    @Test(expected = IOException::class)
    fun pendingJournalBlocksOrdinaryWrites(): Unit =
        runBlocking {
            val gate = MaintenanceGate(temporary.root)
            gate.journal.writeText("pending")
            gate.access { error("must not run") }
        }

    @Test fun recoveryCanClearPendingJournal() =
        runBlocking {
            val gate = MaintenanceGate(temporary.root)
            gate.journal.writeText("pending")
            gate.recover { gate.journal.delete() }
            assertEquals("ready", gate.access { "ready" })
        }

    @Test fun cancelledWaiterDoesNotReleaseAnotherOwnersLock() =
        runBlocking {
            val gate = MaintenanceGate(temporary.root)
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val owner =
                launch {
                    gate.access {
                        entered.complete(Unit)
                        release.await()
                    }
                }
            entered.await()
            val waiting = launch { MaintenanceGate(temporary.root).access { error("must remain locked") } }
            delay(50)
            waiting.cancelAndJoin()
            release.complete(Unit)
            owner.join()
            assertEquals("released", gate.access { "released" })
        }
}
