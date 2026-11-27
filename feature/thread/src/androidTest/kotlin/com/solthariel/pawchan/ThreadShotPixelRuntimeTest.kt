package com.solthariel.pawchan

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.solthariel.pawchan.core.ui.FastScrollGesture
import com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences
import com.solthariel.pawchan.core.ui.PostDisplayPreferences
import com.solthariel.pawchan.core.ui.PostSeparator
import com.solthariel.pawchan.domain.model.MediaDimensions
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.MediaAttachmentSource
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoading
import com.solthariel.pawchan.thread.RichPostTextUi
import com.solthariel.pawchan.thread.ThreadAttachmentActions
import com.solthariel.pawchan.thread.ThreadPostByline
import com.solthariel.pawchan.thread.ThreadPostCard
import com.solthariel.pawchan.thread.ThreadPostDisplay
import com.solthariel.pawchan.thread.ThreadPostMetadataUi
import com.solthariel.pawchan.thread.ThreadPostOptions
import com.solthariel.pawchan.thread.ThreadPostUi
import com.solthariel.pawchan.thread.ThreadShotRow
import com.solthariel.pawchan.thread.ThreadToolsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/** Independent reference: the actual window crop without selection tint, never the cached graphics layer itself. */
class ThreadShotPixelRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun darkPost998MatchesEveryPixelOfVisibleVideoRow() = checkPixels(ThemePreference.DARK)

    @Test fun lightPost998MatchesEveryPixelAfterLeavingComposition() = checkPixels(ThemePreference.LIGHT)

    private fun checkPixels(theme: ThemePreference) {
        lateinit var tools: ThreadToolsState
        lateinit var captureScope: CoroutineScope
        val replies = mutableStateOf("2 ответа")
        val requests = AtomicInteger()
        val bytes =
            InstrumentationRegistry
                .getInstrumentation()
                .context.assets
                .open("threadshot/fixture.png")
                .use { it.readBytes() }
        val client =
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
        val shown = mutableStateOf(true)
        compose.setContent {
            val scope = rememberCoroutineScope()
            tools = remember { ThreadToolsState(scope) }
            captureScope = scope
            ThreadTestTheme(theme) {
                if (shown.value) {
                    // This reference contains the normal background and full list gutter, not a synthetic export frame.
                    Box(Modifier.width(300.dp).background(MaterialTheme.colorScheme.background).testTag("shot-reference")) {
                        ShotTestPost(tools, client, replies.value)
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.waitUntil(10_000L) { requests.get() == 1 }
        // Wait for decoded, non-placeholder pixels and the end of the normal thumbnail fade.
        compose.waitUntil(10_000L) {
            val current = compose.onNodeWithTag("shot-reference").captureToImage().asAndroidBitmap()
            readPixels(current).any { it == FIXTURE_GREEN }
        }
        compose.waitForIdle()
        var reference = compose.onNodeWithTag("shot-reference").captureToImage().asAndroidBitmap()
        compose.runOnIdle { tools.selecting = true }
        compose.waitForIdle()
        if (theme == ThemePreference.LIGHT) {
            // What the user sees at the tap: the row in selection mode, still without the tint. The stored pixels
            // must match exactly this after the row leaves composition.
            reference = compose.onNodeWithTag("shot-reference").captureToImage().asAndroidBitmap()
        }
        compose.runOnIdle { tools.toggleSelection(998L) }
        compose.waitUntil(10_000L) { tools.shots.ready }
        if (theme == ThemePreference.LIGHT) {
            compose.runOnIdle { shown.value = false }
        } else {
            // A live row changed after selection. Use its latest clean window pixels as the reference.
            compose.runOnIdle {
                replies.value = "3 ответа"
                tools.selecting = false
            }
            compose.waitForIdle()
            reference = compose.onNodeWithTag("shot-reference").captureToImage().asAndroidBitmap()
            compose.runOnIdle { tools.selecting = true }
        }
        compose.waitForIdle()
        val saved = compose.runOnIdle { captureScope.async { tools.shots.freeze(listOf(997L, 998L, 999L)) } }
        // The capture runs on the composition's test dispatcher; blocking here would stop it. Let the test clock
        // drive it and read the finished result.
        compose.waitUntil(FREEZE_TIMEOUT_MS) { saved.isCompleted }
        val shot = runBlocking { saved.await() }
        val actual = shot.strips.single()
        assertEquals(reference.width, actual.width)
        assertEquals(reference.height, actual.height)
        val expected = readPixels(reference)
        val pixels = readPixels(actual)
        val diff = ThreadShotDiff(expected, pixels, reference.width)
        val report =
            if (diff.isEmpty) {
                ""
            } else {
                "${diff.describe()}; ${shotFormats(reference, actual)}; saved ${diff.save(theme.name.lowercase(), reference, actual)}"
            }
        assertArrayEquals("Every RGBA pixel must match, no tolerance. $report", expected, pixels)
        assertEquals("No second thumbnail request for export", 1, requests.get())
    }

    private fun readPixels(image: Bitmap): IntArray {
        val software = if (image.config == Bitmap.Config.HARDWARE) checkNotNull(image.copy(Bitmap.Config.ARGB_8888, false)) else image
        return try {
            IntArray(image.width * image.height).also { software.getPixels(it, 0, image.width, 0, 0, image.width, image.height) }
        } finally {
            if (software !== image) software.recycle()
        }
    }
}

@Composable
private fun ShotTestPost(
    tools: ThreadToolsState,
    client: OkHttpClient,
    replies: String,
) {
    val context = LocalContext.current
    val loader = remember { ThumbnailLoader(context, client, diskEnabled = false) }
    DisposableEffect(loader) { onDispose { loader.close() } }
    val post = remember { pixelPost() }
    CompositionLocalProvider(LocalPostDisplayPreferences provides PostDisplayPreferences(showDefaultName = true)) {
        ThreadShotRow(post.id, tools) {
            Column(Modifier.fillMaxWidth()) {
                ThreadPostCard(
                    post,
                    ThumbnailLoading(loader, FastScrollGesture()),
                    ThreadAttachmentActions({}, {}, {}),
                    {},
                    ThreadPostOptions(
                        selecting = tools.selecting,
                        selected = post.id in tools.selected,
                        selectionOutsideRow = true,
                        replyLabel = replies,
                    ),
                )
                PostSeparator(0, 1)
            }
        }
    }
}

private fun pixelPost(): ThreadPostUi {
    val reference = PostLink.Reference(checkNotNull(PostId.parse(2774869L)))
    val text = PostDocument(listOf(PostTextRun(">>2774869", link = reference)))
    val attachment =
        MediaAttachmentUi(
            MediaAttachmentSource("fixture", "clip.mp4", "https://fixture.invalid/clip.mp4", "https://fixture.invalid/preview.png"),
            1_100_000L,
            AttachmentViewer.VIDEO,
            dimensions = MediaDimensions.create(848, 832),
        )
    val byline = ThreadPostByline(date = "24.11.26 15:02:27", defaultName = "Аноним")
    return ThreadPostUi(
        998L,
        text,
        RichPostTextUi.create(text),
        ThreadPostMetadataUi(null, null, false, ThreadPostDisplay(index = 998, byline = byline)),
        listOf(attachment),
    )
}

private val FIXTURE_GREEN = 0xFF22BB7D.toInt()

private const val FREEZE_TIMEOUT_MS = 60_000L
