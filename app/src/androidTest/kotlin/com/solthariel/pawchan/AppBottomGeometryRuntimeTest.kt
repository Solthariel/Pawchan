package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.FAST_SCROLL_BAR_WIDTH
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.navigation.chrome.AppBottomBar
import com.solthariel.pawchan.navigation.chrome.AppBottomDestination
import com.solthariel.pawchan.navigation.chrome.AppNavigationWorkspace
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AppBottomGeometryRuntimeTest(
    private val width: Int,
) {
    @get:Rule val compose = createComposeRule()

    @Test fun fiveTargetsKeepTheirHorizontalBoundsAcrossWorkspaceAndSettings() {
        val selected = mutableStateOf(AppBottomDestination.CATALOG)
        var edgeWidth = 0f
        val actions = AppBottomActions({}, {}, {}, {}, {})
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(width.dp, 600.dp))) {
                edgeWidth = with(LocalDensity.current) { FAST_SCROLL_BAR_WIDTH.toPx() }
                MaterialTheme {
                    Box(Modifier.size(width.dp, 500.dp).testTag("workspace")) {
                        if (selected.value == AppBottomDestination.SETTINGS) {
                            Column(Modifier.fillMaxSize()) {
                                Box(Modifier.weight(1f))
                                AppBottomBar(actions.copy(onWriteClick = null), selected.value)
                            }
                        } else {
                            AppNavigationWorkspace(actions, selected.value) {}
                        }
                    }
                }
            }
        }
        val original = targetBounds()
        val parent = compose.onNodeWithTag("workspace").fetchSemanticsNode().boundsInRoot
        val usableWidth = parent.width - edgeWidth
        original.forEachIndexed { index, bounds ->
            assertEquals(parent.left + usableWidth * (index + 0.5f) / 5f, bounds.center.x, 1f)
        }
        listOf(
            AppBottomDestination.THREADS,
            AppBottomDestination.BOARDS,
            AppBottomDestination.SETTINGS,
            AppBottomDestination.CATALOG,
        ).forEach { destination ->
            compose.runOnIdle { selected.value = destination }
            targetBounds().zip(original).forEach { (current, previous) ->
                assertEquals(previous.left, current.left, 1f)
                assertEquals(previous.right, current.right, 1f)
                assertEquals(previous.center.x, current.center.x, 1f)
            }
        }
    }

    private fun targetBounds(): List<Rect> =
        listOf("Каталог", "Треды", "Доски", "Настройки", "Написать").map {
            compose.onNodeWithContentDescription(it).fetchSemanticsNode().boundsInRoot
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}dp")
        fun cases(): List<Array<Any>> = listOf(320, 393, 600).map { arrayOf(it) }
    }
}
