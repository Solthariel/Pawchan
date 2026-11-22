package com.solthariel.pawchan.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.solthariel.pawchan.domain.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PostingSettingsRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun replyDefaultsFollowFieldsAndUseConciseDescription() {
        var groups = emptyList<SettingsGroup>()
        compose.setContent {
            MaterialTheme {
                val current = postingGroups(AppSettings()) {}
                SideEffect { groups = current }
            }
        }
        compose.runOnIdle {
            assertEquals("Показывать поля формы", groups[0].title)
            assertEquals("Ответы", groups[1].title)
            assertEquals("Новые вложения по умолчанию", groups[2].title)
            val sage = groups[1].items.single() as SettingsItem.Toggle
            assertEquals("Sage по умолчанию", sage.title)
            assertEquals("Если поддерживается сайтом", sage.summary)
        }
    }
}
