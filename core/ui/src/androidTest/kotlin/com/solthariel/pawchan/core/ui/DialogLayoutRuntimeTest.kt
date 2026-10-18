package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DialogLayoutRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun singleFlatPostHasNoDividerAndTwoPostsHaveOnlyOne() {
        val count = mutableStateOf(1)
        val bubbles = mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                val preferences = PostDisplayPreferences(bubblePosts = bubbles.value)
                CompositionLocalProvider(LocalPostDisplayPreferences provides preferences) {
                    Column {
                        repeat(count.value) { index ->
                            Text("Пост $index")
                            PostSeparator(index, count.value - 1)
                        }
                    }
                }
            }
        }
        compose.onAllNodesWithTag("post-list-divider").assertCountEquals(0)
        compose.runOnIdle { count.value = 2 }
        compose.onAllNodesWithTag("post-list-divider").assertCountEquals(1)
        compose.runOnIdle { bubbles.value = true }
        compose.onAllNodesWithTag("post-list-divider").assertCountEquals(0)
    }

    @Test fun menuAndAlertUseTheSameWindowWidthAndMinimumTouchTargets() {
        val menu = mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                if (menu.value) {
                    AppMenuDialog({}) { ActionMenuItem("Действие", {}) }
                } else {
                    AppAlertDialog({}, title = { Text("Вид постов") }, text = { Text("Пузыри") })
                }
            }
        }
        compose.onNodeWithText("Действие").assertHeightIsAtLeast(48.dp)
        val before = compose.onNodeWithTag("app-dialog").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { menu.value = false }
        val after = compose.onNodeWithTag("app-dialog").fetchSemanticsNode().boundsInRoot
        assertEquals(before.width, after.width, 1f)
        assertEquals(before.left, after.left, 1f)
    }
}
