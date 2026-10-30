package com.solthariel.pawchan

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.domain.network.ForumMirrors
import com.solthariel.pawchan.domain.network.MirrorAvailability
import com.solthariel.pawchan.domain.network.MirrorObservation
import com.solthariel.pawchan.domain.network.MirrorState
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.infrastructure.LocalForumMirrors
import com.solthariel.pawchan.settings.MirrorAvailabilitySetting
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MirrorSettingsStyleRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun fullSettingsRowUsesSwitchSemanticsAndSavesTheToggle() {
        var app by mutableStateOf(AppSettings())
        compose.setContent {
            MaterialTheme { Column { MirrorAvailabilitySetting(app) { app = it(app) } } }
        }
        val row = compose.onNodeWithText("Автоматически переключать зеркало")
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)).assertIsOn()
        row.performClick().assertIsOff()
        compose.runOnIdle { assertEquals(false, app.behavior.client.connection.automaticFallback) }
    }

    @Test fun availabilityIsExplicitlyRequestedAndCannotBeRequestedAgainWhileChecking() {
        val mirrors = StyleTestMirrors()
        compose.setContent {
            CompositionLocalProvider(LocalForumMirrors provides mirrors) {
                MaterialTheme { Column { MirrorAvailabilitySetting(AppSettings(), {}) } }
            }
        }
        compose.runOnIdle { assertEquals(0, mirrors.requests) }
        compose.onNodeWithContentDescription("Проверить доступность").performClick()
        compose.onNodeWithContentDescription("Проверить доступность").assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(1, mirrors.requests)
            mirrors.state.value =
                MirrorState(
                    "2ch.su",
                    observations = listOf(MirrorObservation("2ch.su", MirrorAvailability.AVAILABLE, 1_000)),
                )
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Доступно").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("2ch.su").assertCountEquals(1)
        compose.onNodeWithText("Как работает переключение").performClick()
        compose.onNodeWithText("Переключение зеркал").assertIsDisplayed()
        compose.onNodeWithText("Закрыть").performClick()
    }
}

private class StyleTestMirrors : ForumMirrors {
    override val state = MutableStateFlow(MirrorState("2ch.su"))
    var requests = 0

    override fun selectHost(host: String) {
        state.value = state.value.copy(activeHost = host)
    }

    override fun checkAvailability() {
        requests++
        state.value = state.value.copy(checking = true)
    }
}
