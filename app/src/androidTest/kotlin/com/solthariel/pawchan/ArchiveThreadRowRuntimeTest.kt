package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.solthariel.pawchan.arhivach.ArchiveThreadCardUi
import com.solthariel.pawchan.arhivach.ArchiveThreadRow
import com.solthariel.pawchan.core.ui.FastScrollGesture
import com.solthariel.pawchan.domain.archive.ArchiveTag
import com.solthariel.pawchan.domain.archive.ArchiveThreadMetadata
import com.solthariel.pawchan.domain.archive.ArchiveThreadSummary
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoading
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ArchiveThreadRowRuntimeTest {
    @get:Rule val compose = createComposeRule()

    private val http = OkHttpClient()
    private val loader =
        ThumbnailLoader(
            ApplicationProvider.getApplicationContext(),
            http,
            diskEnabled = false,
        )

    @After
    fun closeResources() {
        compose.runOnIdle { loader.close() }
        http.dispatcher.executorService.shutdown()
        http.connectionPool.evictAll()
    }

    @Test
    fun rowUsesTheSharedThreadCardWithoutArchiveOnlyExtras() {
        var opened = 0
        val recentDate = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss"))
        val summary =
            ArchiveThreadSummary(
                id = 55,
                title = "Тема",
                postCount = 23,
                thumbnailUrl = null,
                tags = listOf(ArchiveTag(1, "Тег источника")),
                media = emptyList(),
                metadata =
                    ArchiveThreadMetadata(
                        previewText = "Текст-превью из первого поста",
                        status = "Активный",
                        date = recentDate,
                        updatedAt = "2026-11-14 04:30:00",
                        lastPostAt = "2026-11-14 04:29:57",
                        favorite = true,
                    ),
            )
        compose.setContent {
            val loading = remember { ThumbnailLoading(loader, FastScrollGesture()) }
            MaterialTheme {
                ArchiveThreadRow(
                    ArchiveThreadCardUi(summary, "/b/ - Бред 2ch", media = emptyList()),
                    loading,
                    onClick = { opened++ },
                    onOpenMedia = { _, _ -> },
                    modifier = Modifier.testTag("archive-thread-card"),
                )
            }
        }

        compose.onNodeWithText("Тема").assertIsDisplayed()
        compose.onNodeWithText("Текст-превью из первого поста").assertIsDisplayed()
        compose.onNodeWithText("только что").assertIsDisplayed()
        compose.onNodeWithContentDescription("Постов: 23").assertIsDisplayed()
        compose.onNodeWithContentDescription("Файлов: 0").assertIsDisplayed()
        compose.onNodeWithText("Активный").assertDoesNotExist()
        compose.onNodeWithContentDescription("Архивач: тред активен и обновляется").assertIsDisplayed()
        compose.onNodeWithText("Тег источника").assertDoesNotExist()
        compose.onNodeWithText("Обновлён: 2026-11-14 04:30:00").assertDoesNotExist()
        compose.onNodeWithText("№55").assertDoesNotExist()
        compose.onNodeWithTag("archive-thread-card").performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals(1, opened) }
    }
}
