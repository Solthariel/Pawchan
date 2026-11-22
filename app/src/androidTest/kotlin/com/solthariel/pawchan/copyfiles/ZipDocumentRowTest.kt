package com.solthariel.pawchan.copyfiles

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ZipDocumentRowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun openingAndMoreAreIndependent() {
        var opened = 0
        var menus = 0
        val document = CopyDocument("content://test/1", "thread.zip", "application/zip", 0, -1)
        compose.setContent { MaterialTheme { ZipDocumentRow(document, { opened++ }, { menus++ }) } }
        compose.onNodeWithText("thread.zip").performClick()
        compose.runOnIdle {
            assertEquals(1, opened)
            assertEquals(0, menus)
        }
        compose.onNodeWithContentDescription("Действия с файлом").performClick()
        compose.runOnIdle {
            assertEquals(1, opened)
            assertEquals(1, menus)
        }
    }

    @Test fun missingFileCannotBeOpened() {
        val document = CopyDocument("content://test/2", "missing.zip", "application/zip", 0, -1)
        compose.setContent { MaterialTheme { ZipDocumentRow(document, {}, null, available = false) } }
        compose.onNodeWithText("missing.zip").assertIsNotEnabled()
    }
}
