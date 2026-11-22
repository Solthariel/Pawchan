package com.solthariel.pawchan.composer

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputContentInfo
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ComposerImageInputRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var view: View
    private lateinit var text: TextFieldState
    private val received = mutableListOf<Uri>()
    private val image = Uri.parse("content://pawchan.test/image.png")

    private fun editor() {
        compose.setContent {
            view = LocalView.current
            text = rememberTextFieldState("before")
            MaterialTheme {
                ComposerCommentField(
                    text,
                    ComposerLimits(40 * 1024 * 1024, 4, 15_000),
                    true,
                    remember { FocusRequester() },
                    Modifier.composerMediaReceiver { uris, _ -> received.addAll(uris) }.testTag("image-input"),
                )
            }
        }
        compose.onNodeWithTag("image-input").performClick()
        compose.waitForIdle()
    }

    @Test fun keyboardCommitAdvertisesContentAndAttachesAnImageWithoutInsertingItsUri() {
        editor()
        compose.runOnIdle {
            val info = EditorInfo()
            val connection = view.onCreateInputConnection(info)
            assertNotNull(connection)
            assertTrue(info.contentMimeTypes?.contains("*/*") == true)
            val content = InputContentInfo(image, ClipDescription("image", arrayOf("image/png")), null)
            assertTrue(requireNotNull(connection).commitContent(content, 0, null))
            assertEquals(listOf(image), received)
            assertEquals("before", text.text.toString())
        }
        compose.onNodeWithTag("image-input").performTextInput("after")
        compose.runOnIdle { assertTrue(text.text.toString().contains("after")) }
    }

    @Test fun mixedClipboardPreservesTextAndRoutesOnlyTheImageToAttachments() {
        editor()
        val manager = view.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        var original: ClipData? = null
        try {
            compose.runOnIdle {
                original = manager.primaryClip
                manager.setPrimaryClip(
                    ClipData("mixed", arrayOf("image/png", "text/plain"), ClipData.Item(image)).apply {
                        addItem(ClipData.Item(" caption"))
                    },
                )
            }
            compose.onNodeWithTag("image-input").performSemanticsAction(SemanticsActions.PasteText) { it() }
            compose.waitForIdle()
            compose.runOnIdle {
                assertEquals(listOf(image), received)
                assertTrue(text.text.toString().contains("caption"))
                assertTrue(!text.text.toString().contains("content://"))
            }
        } finally {
            compose.runOnIdle { original?.let(manager::setPrimaryClip) ?: manager.clearPrimaryClip() }
        }
    }
}
