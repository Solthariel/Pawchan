package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.data.network.proxy.ProxyFailure
import com.solthariel.pawchan.data.network.proxy.ProxyProbeResult
import com.solthariel.pawchan.domain.proxy.ProxyMode
import com.solthariel.pawchan.domain.proxy.ProxyService
import com.solthariel.pawchan.domain.proxy.SavedProxy
import com.solthariel.pawchan.settings.ProxyCheckDialog
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class ProxyCheckDialogRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private val draft =
        SavedProxy(
            "id",
            "name",
            ProxyMode.HTTP,
            "localhost:8080",
            true,
            setOf(ProxyService.DVACH, ProxyService.FOUR_CHAN),
        )

    @Test fun dialogShowsProgressResultsAndRepeatsOnlySelectedResources() {
        val gate = CompletableDeferred<Unit>()
        val calls = AtomicInteger()
        var open by mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                if (open) {
                    ProxyCheckDialog(draft, "2ch.su", { _, service, _ ->
                        calls.incrementAndGet()
                        if (service == ProxyService.DVACH) {
                            gate.await()
                            ProxyProbeResult.Response(200, 320)
                        } else {
                            ProxyProbeResult.Failed(ProxyFailure.TIMEOUT)
                        }
                    }) { open = false }
                }
            }
        }
        compose.onNodeWithTag("proxy-check-dialog").assertIsDisplayed()
        compose.onNodeWithText("2ch: Проверяется…").assertIsDisplayed()
        compose.onNodeWithText("4chan: Ожидает проверки").assertIsDisplayed()
        compose.onNodeWithText("Повторить").assertDoesNotExist()
        gate.complete(Unit)
        compose.waitUntil(3000) { calls.get() == 2 }
        compose.onNodeWithText("2ch: Ответ за 320 мс").assertIsDisplayed()
        compose.onNodeWithText("4chan: Время ожидания истекло").assertIsDisplayed()
        compose.onNodeWithText("Повторить").performClick()
        compose.waitUntil(3000) { calls.get() == 4 }
        compose.onNodeWithText("Закрыть").performClick()
        compose.onNodeWithTag("proxy-check-dialog").assertDoesNotExist()
        assertEquals(4, calls.get())
    }

    @Test fun closingTheDialogCancelsTheActiveRequest() {
        val started = AtomicBoolean(false)
        val cancelled = AtomicBoolean(false)
        var open by mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                if (open) {
                    ProxyCheckDialog(draft, "2ch.su", { _, _, _ ->
                        started.set(true)
                        try {
                            awaitCancellation()
                        } finally {
                            cancelled.set(true)
                        }
                    }) { open = false }
                }
            }
        }
        compose.waitUntil(3000) { started.get() }
        compose.onNodeWithText("Закрыть").performClick()
        compose.waitUntil(3000) { cancelled.get() }
        compose.onNodeWithTag("proxy-check-dialog").assertDoesNotExist()
    }
}
