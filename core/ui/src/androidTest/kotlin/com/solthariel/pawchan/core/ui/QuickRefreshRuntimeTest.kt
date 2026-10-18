package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.BidirectionalPullRefreshBox
import com.solthariel.pawchan.core.ui.PullRefreshOptions
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class QuickRefreshRuntimeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun synchronousCachedErrorDoesNotLeavePullSpinnerRunning() {
        var requests = 0
        compose.setContent {
            MaterialTheme {
                BidirectionalPullRefreshBox(
                    refreshing = false,
                    onRefresh = { requests++ },
                    options = PullRefreshOptions(allowBottom = false),
                ) {
                    LazyColumn(Modifier.fillMaxSize().testTag("refresh-list")) {
                        items(50) { Text("Row $it", Modifier.height(60.dp)) }
                    }
                }
            }
        }
        compose.onNodeWithTag("refresh-list").performTouchInput {
            down(Offset(center.x, height * 0.1f))
            moveTo(Offset(center.x, height * 0.85f), delayMillis = 500)
            up()
        }
        compose.mainClock.advanceTimeBy(250)
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1, requests) }
        val progress = SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)
        compose.onNode(progress).assertDoesNotExist()
    }
}
