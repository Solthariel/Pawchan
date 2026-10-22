package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.thread.LocalThreadArchive
import com.solthariel.pawchan.thread.ThreadArchiveHost
import com.solthariel.pawchan.thread.ThreadArhivachItems
import com.solthariel.pawchan.thread.ThreadDialog
import com.solthariel.pawchan.thread.ThreadExternalArchive
import com.solthariel.pawchan.thread.ThreadExternalArchiveState
import com.solthariel.pawchan.thread.ThreadToolsState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ArhivachRetryRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun failedAvailabilityProbeDoesNotPreventOpeningAndReopeningTheCaptcha() {
        val available = mutableStateOf(true)
        lateinit var tools: ThreadToolsState
        compose.setContent {
            val scope = rememberCoroutineScope()
            tools = androidx.compose.runtime.remember { ThreadToolsState(scope) }
            val external =
                ThreadExternalArchive(
                    state = { ThreadExternalArchiveState(available = available.value) },
                    submitDialog = { _, _ -> },
                    open = {},
                    load = { _, _ -> error("not a load action") },
                )
            MaterialTheme {
                CompositionLocalProvider(LocalThreadArchive provides ThreadArchiveHost({ _, _ -> }, external)) {
                    ThreadArhivachItems(address(), tools, null, scope)
                }
            }
        }
        compose.runOnIdle { available.value = false }
        compose.onNodeWithText("Сохранить в Архивач").assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(ThreadDialog.ARHIVACH, tools.dialog)
            tools.dialog = ThreadDialog.NONE
        }
        compose.onNodeWithText("Сохранить в Архивач").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(ThreadDialog.ARHIVACH, tools.dialog) }
    }

    private fun address() =
        ThreadAddress(
            BoardAddress(requireNotNull(SiteId.parse("2ch")), requireNotNull(BoardId.parse("b"))),
            requireNotNull(ThreadId.parse(1)),
        )
}
