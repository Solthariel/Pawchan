package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class VideoControlAlignmentRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun videoButtonsUsePhotoCellWidthAndAreCentered() {
        compose.setContent {
            MaterialTheme {
                Column(Modifier.width(320.dp)) {
                    AttachmentControlRow(6) {
                        repeat(6) { index -> Box(Modifier.weight(1f).height(40.dp).testTag("photo:$index")) }
                    }
                    AttachmentControlRow(3, centeredLikeSix = true) {
                        repeat(3) { index -> Box(Modifier.weight(1f).height(40.dp).testTag("video:$index")) }
                    }
                    AttachmentFileSummary(ComposerAttachment(ReplyAttachment("clip.webm", "video/webm", byteArrayOf(1, 2))))
                }
            }
        }
        val photo = compose.onNodeWithTag("photo:0").fetchSemanticsNode().boundsInRoot
        val first = compose.onNodeWithTag("video:0").fetchSemanticsNode().boundsInRoot
        val middle = compose.onNodeWithTag("video:1").fetchSemanticsNode().boundsInRoot
        val last = compose.onNodeWithTag("video:2").fetchSemanticsNode().boundsInRoot
        val photoFirst = compose.onNodeWithTag("photo:0").fetchSemanticsNode().boundsInRoot
        val photoLast = compose.onNodeWithTag("photo:5").fetchSemanticsNode().boundsInRoot
        assertEquals(photo.width, first.width, 2f)
        assertEquals((photoFirst.left + photoLast.right) / 2f, (first.left + last.right) / 2f, 2f)
        assertEquals(middle.left - first.right, last.left - middle.right, 2f)
        compose.onNodeWithText("clip.webm", substring = true).assertExists()
    }
}
