package com.solthariel.pawchan.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PostHeaderRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun sharedHeaderKeepsIndexNumberDateAndAuthorOrder() {
        compose.setContent {
            MaterialTheme {
                val secondary = MaterialTheme.colorScheme.onSurfaceVariant
                PostHeaderLayout(
                    leading = {
                        PostHeaderIndex(deleted = false, index = 1)
                        PostHeaderText("#42", secondary)
                        PostHeaderText("01.01.26", secondary)
                    },
                    byline = { PostHeaderAuthor(PostHeaderAuthorUi(name = "Автор")) },
                )
            }
        }
        val left =
            listOf("1", "#42", "01.01.26", "Автор").map {
                compose
                    .onNodeWithText(it)
                    .fetchSemanticsNode()
                    .boundsInRoot.left
            }
        assertTrue(left.zipWithNext().all { (a, b) -> a < b })
    }

    @Test
    fun deletedIndexKeepsItsAccessibilityLabel() {
        compose.setContent { MaterialTheme { PostHeaderIndex(deleted = true, index = 1) } }
        compose.onNodeWithContentDescription("Удалён").assertExists()
        compose.onNodeWithText("X").assertExists()
    }
}
