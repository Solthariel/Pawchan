package com.solthariel.pawchan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.PreservedMediaContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MediaContentRestoreRuntimeTest {
    @get:Rule
    val compose = createComposeRule()

    private var viewer by mutableStateOf(AttachmentViewer.NONE)
    private lateinit var state: LazyListState
    private lateinit var scope: CoroutineScope

    @Test
    fun imageReturnKeepsTheItemOffsetAndSearch() {
        compose.setContent { Fixture() }
        openViewer(AttachmentViewer.IMAGE)
        assertRestored()
    }

    @Test
    fun videoReturnKeepsTheItemOffsetAndSearch() {
        compose.setContent { Fixture() }
        openViewer(AttachmentViewer.VIDEO)
        assertRestored()
    }

    @Test
    fun savedInstanceStateWhileViewerIsOpenRetainsTheHiddenList() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { Fixture() }
        openViewer(AttachmentViewer.IMAGE)
        restoration.emulateSavedInstanceStateRestore()
        assertRestored()
    }

    @Test
    fun repeatedImageAndVideoReturnsDoNotReplaceSavedPositionWithZero() {
        compose.setContent { Fixture() }
        for (kind in listOf(AttachmentViewer.IMAGE, AttachmentViewer.VIDEO, AttachmentViewer.IMAGE)) {
            openViewer(kind)
            assertRestored()
        }
    }

    private fun openViewer(kind: AttachmentViewer) {
        compose.onNodeWithTag("media-search").performTextReplacement("kept-query")
        compose.runOnIdle { scope.launch { state.scrollToItem(40, 17) } }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(40, state.firstVisibleItemIndex)
            assertEquals(17, state.firstVisibleItemScrollOffset)
            viewer = kind
        }
        compose.onNodeWithTag("media-list").assertExists()
        compose.onNodeWithTag("close-viewer").assertIsDisplayed()
    }

    private fun assertRestored() {
        compose.onNodeWithTag("close-viewer").performClick()
        compose.onNodeWithTag("media-list").assertIsDisplayed()
        compose.onNodeWithTag("media-search").assertTextEquals("kept-query")
        compose.runOnIdle {
            assertEquals(40, state.firstVisibleItemIndex)
            assertEquals(17, state.firstVisibleItemScrollOffset)
        }
    }

    @Composable
    private fun Fixture() {
        MaterialTheme {
            PreservedMediaContent(
                visible = viewer == AttachmentViewer.NONE,
                content = { ListContent() },
                viewer = {
                    Button(onClick = { viewer = AttachmentViewer.NONE }, modifier = Modifier.testTag("close-viewer")) {
                        Text("Close ${viewer.name}")
                    }
                },
            )
        }
    }

    @Composable
    private fun ListContent() {
        state = rememberLazyListState()
        scope = rememberCoroutineScope()
        var query by rememberSaveable { mutableStateOf("") }
        Column(Modifier.fillMaxSize()) {
            BasicTextField(query, onValueChange = { query = it }, modifier = Modifier.testTag("media-search"))
            LazyColumn(Modifier.weight(1f).testTag("media-list"), state = state) {
                items(120, key = { it }) { Text("Row $it", Modifier.height(60.dp)) }
            }
        }
    }
}
