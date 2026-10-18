package com.solthariel.pawchan

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.AnnotatedString
import com.solthariel.pawchan.thread.PostLinkPressFeedback
import com.solthariel.pawchan.thread.PostTextGestures
import com.solthariel.pawchan.thread.REFERENCE_TAG
import com.solthariel.pawchan.thread.postLinkPressHighlight
import com.solthariel.pawchan.thread.postTextGestures
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class PostLinkPressRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun downHighlightsReferenceAndUpClearsHighlight() {
        val feedback = showReference()
        compose.onNodeWithTag("reference").performTouchInput { down(center) }
        compose.runOnIdle { assertNotNull(feedback.path) }
        compose.onNodeWithTag("reference").performTouchInput { up() }
        compose.runOnIdle { assertNull(feedback.path) }
    }

    @Test
    fun cancelledGestureClearsHighlight() {
        val feedback = showReference()
        compose.onNodeWithTag("reference").performTouchInput { down(center) }
        compose.runOnIdle { assertNotNull(feedback.path) }
        compose.onNodeWithTag("reference").performTouchInput { cancel() }
        compose.runOnIdle { assertNull(feedback.path) }
    }

    private fun showReference(): PostLinkPressFeedback {
        val text =
            AnnotatedString
                .Builder(">>456")
                .apply {
                    addStringAnnotation(REFERENCE_TAG, "456", 0, 5)
                }.toAnnotatedString()
        val feedback = PostLinkPressFeedback(text, 0f)
        val gestures = PostTextGestures({}, {}, {}, feedback::press, feedback::release)
        compose.setContent {
            Text(
                text,
                modifier =
                    Modifier
                        .testTag("reference")
                        .postLinkPressHighlight(feedback, Color.Blue)
                        .postTextGestures(text, false, gestures),
                onTextLayout = feedback::updateLayout,
            )
        }
        return feedback
    }
}
