package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.feature.composer.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ComposerHeaderGeometryRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun attachmentTargetEndsAtTheSameInsetAsTheSharedToolbar() {
        var attaches = 0
        var label = ""
        compose.setContent {
            MaterialTheme {
                label = stringResource(R.string.attachment_add)
                Box(Modifier.width(320.dp)) {
                    ComposerScreenHeader(
                        "Новое сообщение в /b/",
                        ComposerScreenState(
                            ComposerDraftState(),
                            null,
                            ComposerCaptchaState(),
                            ComposerScreenStatus(ComposerProgressState(), ComposerFailureState(), false),
                        ),
                        true,
                        {},
                        { attaches++ },
                        {},
                    )
                }
            }
        }
        val target = compose.onNodeWithContentDescription(label)
        target.assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
        val bounds = target.fetchSemanticsNode().boundsInRoot
        assertEquals(316f * compose.density.density, bounds.right, 1f)
        target.performClick()
        compose.runOnIdle { assertEquals(1, attaches) }
    }
}
