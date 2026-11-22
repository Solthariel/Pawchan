package com.solthariel.pawchan

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import coil3.ImageLoader
import com.solthariel.pawchan.arhivach.ArchiveBrowseState
import com.solthariel.pawchan.arhivach.ArchiveOpenThreadState
import com.solthariel.pawchan.arhivach.ArchivePostList
import com.solthariel.pawchan.domain.archive.ArchiveMedia
import com.solthariel.pawchan.domain.archive.ArchivePost
import com.solthariel.pawchan.domain.archive.ArchiveThread
import com.solthariel.pawchan.infrastructure.MediaLoaders
import com.solthariel.pawchan.infrastructure.media.MediaDownloadQueue
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Rule
import org.junit.Test

class ArchiveGalleryRuntimeTest {
    @get:Rule val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val http = OkHttpClient()
    private val imageLoader = ImageLoader.Builder(context).build()
    private val thumbnails = ThumbnailLoader(context, http, diskEnabled = false)

    @After
    fun closeResources() {
        compose.runOnIdle { thumbnails.close() }
        imageLoader.shutdown()
        http.dispatcher.executorService.shutdown()
        http.connectionPool.evictAll()
    }

    @Test
    fun requestOpensSharedGalleryInsteadOfTheFirstAttachment() {
        val media = listOf("first.jpg", "second.mp4").map { ArchiveMedia("https://example.invalid/$it", null) }
        val thread = ArchiveThread(55, "Тред", listOf(ArchivePost("55", null, null, "Текст", media)))
        val state = ArchiveBrowseState(openThread = ArchiveOpenThreadState(55, thread, galleryEnabled = true))
        val queue = MediaDownloadQueue(context, (context.applicationContext as PawchanApplication).preferences)
        val loaders = MediaLoaders(imageLoader, thumbnails, queue, http)
        var request by mutableIntStateOf(0)
        compose.setContent {
            MaterialTheme { ArchivePostList(state, loaders, "", request, Modifier.fillMaxSize()) }
        }
        compose.runOnIdle { request++ }
        compose.waitUntil(timeoutMillis = 5_000) {
            compose
                .onAllNodes(
                    androidx.compose.ui.test
                        .hasTestTag("gallery-toolbar"),
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithTag("gallery-toolbar").assertIsDisplayed()
        compose.onNodeWithContentDescription("Все: 2").assertIsDisplayed()
    }
}
