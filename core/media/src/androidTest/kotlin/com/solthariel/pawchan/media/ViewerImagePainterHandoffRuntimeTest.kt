package com.solthariel.pawchan.media

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import coil3.asImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberConstraintsSizeResolver
import coil3.decode.DataSource
import coil3.intercept.Interceptor
import coil3.request.SuccessResult
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** Non-shareable results model GIF ownership: a memory cache hit must not conceal a second request. */
class ViewerImagePainterHandoffRuntimeTest {
    @get:Rule
    val compose = createComposeRule()
    private val requests = ConcurrentHashMap<String, AtomicInteger>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val loader =
        ImageLoader
            .Builder(context)
            .components {
                add(
                    Interceptor { chain ->
                        val url = chain.request.data.toString()
                        requests.getOrPut(url) { AtomicInteger() }.incrementAndGet()
                        SuccessResult(
                            image = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).asImage(shareable = false),
                            request = chain.request,
                            dataSource = DataSource.NETWORK,
                        )
                    },
                )
            }.build()
    private var current by mutableStateOf("still")
    private var retry by mutableIntStateOf(0)
    private var neighbors by mutableStateOf(listOf("gif"))
    private var painters = emptyMap<String, AsyncImagePainter>()

    @After
    fun close() = loader.shutdown()

    private fun content() {
        compose.setContent {
            val resolver = rememberConstraintsSizeResolver()
            painters = rememberViewerImagePainters(viewerImagePageKeys(current, null, neighbors, retry), loader, resolver)
            Box(Modifier.size(120.dp).then(resolver)) { Image(painters.getValue(current), null) }
        }
        compose.waitUntil { painters.size == 2 && painters.values.all { it.state.value is AsyncImagePainter.State.Success } }
    }

    @Test
    fun selectingAndReturningDoNotRestartNonShareableImage() {
        content()
        val gif = painters.getValue("gif")
        val still = painters.getValue("still")
        repeat(4) {
            compose.runOnIdle {
                current = "gif"
                neighbors = listOf("still")
            }
            compose.runOnIdle {
                assertSame(gif, painters.getValue("gif"))
                assertSame(still, painters.getValue("still"))
            }
            compose.runOnIdle {
                current = "still"
                neighbors = listOf("gif")
            }
        }
        compose.runOnIdle {
            assertEquals(1, requests.getValue("gif").get())
            assertEquals(1, requests.getValue("still").get())
        }
    }

    @Test
    fun refreshReplacesOnlyTheSelectedPainter() {
        content()
        val selected = painters.getValue("still")
        val neighbor = painters.getValue("gif")
        compose.runOnIdle { retry++ }
        compose.waitUntil { requests.getValue("still").get() == 2 }
        compose.runOnIdle {
            assertNotSame(selected, painters.getValue("still"))
            assertSame(neighbor, painters.getValue("gif"))
            assertEquals(1, requests.getValue("gif").get())
        }
    }

    @Test
    fun movingPastTheNeighborReleasesItsRememberedPainter() {
        content()
        val old = painters.getValue("gif")
        compose.runOnIdle { neighbors = listOf("third") }
        compose.waitUntil { painters["third"]?.state?.value is AsyncImagePainter.State.Success }
        compose.runOnIdle { neighbors = listOf("gif") }
        compose.waitUntil { requests.getValue("gif").get() == 2 }
        compose.runOnIdle { assertNotSame(old, painters.getValue("gif")) }
    }
}
