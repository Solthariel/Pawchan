package com.solthariel.pawchan

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.solthariel.pawchan.settings.ProxyEditorFields
import com.solthariel.pawchan.settings.ProxyEditorState
import com.solthariel.pawchan.settings.newProxy
import org.junit.Rule
import org.junit.Test

class ProxyAddressPasteRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun insertingHostAndPortUpdatesBothFields() {
        compose.setContent {
            MaterialTheme { Column { ProxyEditorFields(remember { ProxyEditorState(newProxy()) }) } }
        }
        compose.onNodeWithText("Адрес").performTextInput("192.0.2.1:1080")
        compose.onNodeWithText("Адрес").assertTextContains("192.0.2.1")
        compose.onNodeWithText("Порт").assertTextContains("1080")
    }
}
