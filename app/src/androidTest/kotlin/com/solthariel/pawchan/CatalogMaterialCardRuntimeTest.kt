package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.catalog.CatalogMediaClicks
import com.solthariel.pawchan.catalog.CatalogThreadCard
import com.solthariel.pawchan.catalog.CatalogThreadContentUi
import com.solthariel.pawchan.catalog.CatalogThreadFlagsUi
import com.solthariel.pawchan.catalog.CatalogThreadStatisticsUi
import com.solthariel.pawchan.catalog.CatalogThreadTextUi
import com.solthariel.pawchan.catalog.CatalogThreadUi
import com.solthariel.pawchan.core.ui.FastScrollGesture
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoading
import com.solthariel.pawchan.navigation.defaultBoardAddress
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CatalogMaterialCardRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private val scale = mutableFloatStateOf(1f)
    private var opens = 0
    private var menus = 0

    @Test fun cardGrowsWithFontInsteadOfClippingAt110dp() {
        show()
        val normal = compose.onNodeWithTag("material-thread-card").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { scale.floatValue = 2f }
        val grown = compose.onNodeWithTag("material-thread-card").fetchSemanticsNode().boundsInRoot
        val metadata = compose.onNodeWithContentDescription("Ответов: 17").fetchSemanticsNode().boundsInRoot
        assertTrue(grown.height > normal.height)
        assertTrue(metadata.bottom <= grown.bottom)
        assertTrue(metadata.top >= grown.top)
        compose.onNodeWithText("Тема каталога").assertExists()
    }

    @Test fun normalTapAndLongPressKeepSeparateActions() {
        show()
        compose.onNodeWithTag("material-thread-card").performTouchInput { click() }
        compose.runOnIdle {
            assertEquals(1, opens)
            assertEquals(0, menus)
        }
        compose.onNodeWithTag("material-thread-card").performTouchInput { longClick() }
        compose.runOnIdle {
            assertEquals(1, opens)
            assertEquals(1, menus)
        }
    }

    @Test fun hiddenThreadRemainsReadableAndCanBeOpenedAtLargeFont() {
        scale.floatValue = 2f
        show(hidden = true)
        compose.onNodeWithText("Скрытый тред #42").assertExists()
        compose.onNodeWithTag("material-thread-card").performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, opens) }
    }

    private fun show(hidden: Boolean = false) {
        val thread =
            CatalogThreadUi(
                ThreadAddress(defaultBoardAddress(), requireNotNull(ThreadId.parse(42))),
                CatalogThreadContentUi("Тема каталога", CatalogThreadTextUi("Подробности обсуждения."), null, null, false),
                emptyList(),
                CatalogThreadFlagsUi(false, false, false),
                CatalogThreadStatisticsUi(17, 2, 0L, false),
                hidden,
            )
        compose.setContent {
            val context = LocalContext.current
            val density = LocalDensity.current
            val loader = remember { ThumbnailLoader(context, OkHttpClient()) }
            DisposableEffect(loader) { onDispose { loader.close() } }
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale.floatValue)) {
                MaterialTheme {
                    Box(Modifier.width(300.dp)) {
                        CatalogThreadCard(
                            thread,
                            ThumbnailLoading(loader, FastScrollGesture()),
                            { opens++ },
                            CatalogMediaClicks({}, {}),
                            Modifier.testTag("material-thread-card"),
                            { menus++ },
                        )
                    }
                }
            }
        }
    }
}
