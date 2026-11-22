package com.solthariel.pawchan.composer

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AttachmentQualityErrorRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun sliderExposesAndUpdatesQualityAtTheMinimum() {
        val quality = mutableStateOf(100)
        compose.setContent { MaterialTheme { AttachmentQualityControl(quality.value, true) { quality.value = it } } }
        compose.onNodeWithContentDescription("Качество JPEG").assertExists()
        compose.onNodeWithText("100%").assertExists()
        compose
            .onNodeWithContentDescription("Качество JPEG")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        compose.runOnIdle { assertEquals(1, quality.value) }
        compose.onNodeWithText("1%").assertExists()
    }
}
