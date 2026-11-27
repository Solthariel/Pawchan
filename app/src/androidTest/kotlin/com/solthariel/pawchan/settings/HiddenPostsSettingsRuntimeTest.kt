package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.solthariel.pawchan.domain.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HiddenPostsSettingsRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun postAndThreadSwitchesAreIndependent() {
        var settings by mutableStateOf(AppSettings())
        var sections = emptyList<SettingsGroup>()
        compose.setContent {
            MaterialTheme {
                val groups =
                    settingsContent(
                        SettingsUiState(app = settings),
                        SettingsActions({}, { change -> settings = change(settings) }, {}),
                        SettingsEnvironment(null, {}),
                    ).getValue(SettingsSection.INTERFACE)
                SideEffect { sections = groups }
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    SettingsGroupCard(groups.single { it.title == "Отображение" })
                }
            }
        }
        val stickers = compose.onNode(hasText("Скрывать стикеры") and isToggleable())
        val monkey = compose.onNode(hasText("Скрывать ответы @monkey") and isToggleable())
        val threads = compose.onNode(hasText("Выводить скрытые треды") and isToggleable())
        val posts = compose.onNode(hasText("Выводить скрытые посты") and isToggleable())
        stickers
            .performScrollTo()
            .assertIsOff()
            .performClick()
            .assertIsOn()
        monkey
            .performScrollTo()
            .assertIsOff()
            .performClick()
            .assertIsOn()
        posts
            .performScrollTo()
            .assertIsOn()
            .performClick()
            .assertIsOff()
        threads
            .performScrollTo()
            .assertIsOff()
            .performClick()
            .assertIsOn()
        posts
            .performScrollTo()
            .assertIsOff()
            .performClick()
            .assertIsOn()
        threads.assertIsOn()
        stickers.assertIsOn()
        monkey.assertIsOn()
        compose.runOnIdle {
            val expected =
                setOf("Скрывать стикеры", "Скрывать ответы @monkey", "Выводить скрытые треды", "Выводить скрытые посты")
            expected.forEach { name ->
                val owners = sections.flatMap { group -> group.items.filter { it.title == name }.map { group.title } }
                assertEquals(listOf("Отображение"), owners)
            }
        }
    }
}
