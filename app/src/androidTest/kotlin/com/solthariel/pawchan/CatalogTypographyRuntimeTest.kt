package com.solthariel.pawchan

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.solthariel.pawchan.catalog.CatalogThreadAgeUi
import com.solthariel.pawchan.catalog.CatalogThreadCard
import com.solthariel.pawchan.catalog.CatalogThreadContentUi
import com.solthariel.pawchan.catalog.CatalogThreadFlagsUi
import com.solthariel.pawchan.catalog.CatalogThreadStatisticsUi
import com.solthariel.pawchan.catalog.CatalogThreadTextUi
import com.solthariel.pawchan.catalog.CatalogThreadUi
import com.solthariel.pawchan.core.ui.FastScrollGesture
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.settings.AppFont
import com.solthariel.pawchan.domain.settings.TypographyPreferences
import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.MediaAttachmentSource
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoading
import com.solthariel.pawchan.navigation.defaultBoardAddress
import com.solthariel.pawchan.threads.ThreadListCardActions
import com.solthariel.pawchan.ui.appTypography
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CatalogTypographyRuntimeTest(
    private val font: AppFont,
    private val fontScale: Float,
) {
    @get:Rule val compose = createComposeRule()
    private val http = OkHttpClient()
    private val loader =
        ThumbnailLoader(ApplicationProvider.getApplicationContext<Context>(), http, diskEnabled = false)

    @After fun close() {
        compose.runOnIdle { loader.close() }
        http.dispatcher.executorService.shutdown()
        http.connectionPool.evictAll()
    }

    @Test
    fun titleMetadataAndCardInteractionRemainUsable() {
        var opened = 0
        val thread = sampleThread()
        compose.setContent {
            val density = LocalDensity.current
            val loading = remember { ThumbnailLoading(loader, FastScrollGesture()) }
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MaterialTheme(typography = appTypography(TypographyPreferences(font = font))) {
                    Box(Modifier.width(320.dp)) {
                        CatalogThreadCard(
                            thread,
                            loading,
                            ThreadListCardActions(onClick = { opened++ }, onOpenMedia = { _, _ -> }),
                            Modifier.testTag("catalog-card"),
                        )
                    }
                }
            }
        }
        val title = compose.onNodeWithText("Длинный заголовок тестового треда", useUnmergedTree = true)
        val age = compose.onNodeWithText("5 мин назад", useUnmergedTree = true)
        compose.onNodeWithText(thread.statistics.sizeText, useUnmergedTree = true).assertDoesNotExist()
        title.assertIsDisplayed()
        age.assertIsDisplayed()
        compose.onNodeWithContentDescription("Ответов: 17", useUnmergedTree = true).assertIsDisplayed()
        assertTrue(title.fetchSemanticsNode().boundsInRoot.bottom <= age.fetchSemanticsNode().boundsInRoot.top)
        compose.onNodeWithTag("catalog-card").performClick()
        compose.runOnIdle { assertEquals(1, opened) }
    }

    private fun sampleThread(): CatalogThreadUi =
        CatalogThreadUi(
            address = ThreadAddress(defaultBoardAddress(), requireNotNull(ThreadId.parse(42))),
            content =
                CatalogThreadContentUi(
                    "Длинный заголовок тестового треда",
                    CatalogThreadTextUi("Текст первого поста. Проверяем расположение элементов в карточке."),
                    null,
                    null,
                    false,
                ),
            attachments =
                listOf(
                    MediaAttachmentUi(
                        MediaAttachmentSource("42", "image.jpg", "https://example.org/image.jpg", null),
                        null,
                        AttachmentViewer.IMAGE,
                    ),
                ),
            flags = CatalogThreadFlagsUi(false, false, false),
            statistics = CatalogThreadStatisticsUi(17, 2, 2_000_000L, true),
            age = CatalogThreadAgeUi(1_800_000_000L, "5 мин назад"),
        )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}, scale={1}")
        fun cases(): List<Array<Any>> = AppFont.entries.flatMap { font -> listOf(1f, 1.6f).map { arrayOf(font, it) } }
    }
}
