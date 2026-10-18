package com.solthariel.pawchan

import android.graphics.Bitmap
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.solthariel.pawchan.core.ui.PawchanFastScrollbar
import com.solthariel.pawchan.media.thumbnail.DeferredThumbnail
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoading
import com.solthariel.pawchan.media.thumbnail.rememberThumbnailLoading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/** Real Lazy subscriptions and HTTP fixture; requests must start before scrolling ends. */
class ThumbnailScrollingRuntimeTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var state: LazyListState
    private lateinit var scope: CoroutineScope
    private lateinit var loading: ThumbnailLoading
    private lateinit var loader: ThumbnailLoader
    private lateinit var client: OkHttpClient
    private val requests = AtomicInteger()
    private var showThumbnails by mutableStateOf(false)
    private val url = "https://example.invalid/${UUID.randomUUID()}.png"

    @Before
    fun prepare() {
        val bytes =
            ByteArrayOutputStream().use { output ->
                val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                bitmap.recycle()
                output.toByteArray()
            }
        client =
            OkHttpClient
                .Builder()
                .addInterceptor { chain ->
                    requests.incrementAndGet()
                    Response
                        .Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("fixture")
                        .body(bytes.toResponseBody())
                        .build()
                }.build()
        compose.runOnUiThread {
            loader = ThumbnailLoader(InstrumentationRegistry.getInstrumentation().targetContext, client)
        }
    }

    @After
    fun close() {
        compose.runOnIdle { showThumbnails = false }
        compose.runOnIdle { loader.close() }
        client.dispatcher.executorService.shutdownNow()
        client.connectionPool.evictAll()
    }

    @Test
    fun continuousSlowScrollStartsNetworkWithoutAnIdleWindow() {
        showList()
        lateinit var motion: Job
        compose.runOnIdle {
            motion =
                scope.launch {
                    state.scroll(MutatePriority.UserInput) {
                        showThumbnails = true
                        repeat(2_000) {
                            scrollBy(1f)
                            delay(16)
                        }
                    }
                }
        }
        try {
            compose.waitUntil(timeoutMillis = 5_000) { requests.get() > 0 }
            compose.runOnIdle {
                assertTrue(state.isScrollInProgress)
                assertTrue(state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 0)
                assertEquals(1, requests.get())
            }
        } finally {
            compose.runOnIdle { motion.cancel() }
        }
    }

    @Test
    fun heldListPointerStartsNetworkBeforePointerUp() {
        showList()
        val list = compose.onNodeWithTag("scrolling-list")
        list.performTouchInput {
            down(center)
            moveTo(Offset(center.x, center.y - height * 0.2f), delayMillis = 200)
        }
        try {
            compose.runOnIdle {
                assertTrue(state.isScrollInProgress)
                showThumbnails = true
            }
            compose.waitUntil(timeoutMillis = 5_000) { requests.get() > 0 }
            compose.runOnIdle { assertTrue(state.isScrollInProgress) }
        } finally {
            list.performTouchInput { up() }
        }
    }

    @Test
    fun movingScrollbarStartsNetworkBeforeFinish() {
        showList()
        val scrollbar = compose.onNodeWithTag("thumbnail-scrollbar")
        scrollbar.performTouchInput { down(center) }
        try {
            compose.runOnIdle { showThumbnails = true }
            var step = 0
            compose.waitUntil(timeoutMillis = 5_000) {
                scrollbar.performTouchInput {
                    moveTo(Offset(center.x, center.y + step++ % 100), delayMillis = 16)
                }
                requests.get() > 0
            }
            compose.runOnIdle { assertTrue(loading.gesture.dragging) }
        } finally {
            scrollbar.performTouchInput { up() }
        }
    }

    private fun showList() {
        compose.setContent {
            MaterialTheme {
                state = rememberLazyListState()
                scope = rememberCoroutineScope()
                loading = rememberThumbnailLoading(loader, state)
                Box(Modifier.fillMaxSize()) {
                    LazyColumn(Modifier.fillMaxSize().testTag("scrolling-list"), state = state) {
                        items(80, key = { it }, contentType = { "row" }) { index ->
                            Row(Modifier.height(60.dp)) {
                                Text("Row $index")
                                if (showThumbnails) {
                                    DeferredThumbnail(url, loading, "Preview", Modifier.size(32.dp))
                                }
                            }
                        }
                    }
                    PawchanFastScrollbar(
                        state,
                        Modifier.align(Alignment.CenterEnd).testTag("thumbnail-scrollbar"),
                        inputGesture = loading.gesture,
                    )
                }
            }
        }
    }
}
