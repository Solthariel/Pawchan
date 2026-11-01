package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.posting.PostingIcon
import com.solthariel.pawchan.domain.posting.PostingIdentityRules
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.feature.composer.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ComposerSingleRowRuntimeTest(
    private val width: Int,
    private val fontScale: Float,
    private val title: String,
) {
    @get:Rule val compose = createComposeRule()

    @Test fun allHeaderActionsStayInOneRowWithSingleLineTitle() {
        var labels = emptyList<String>()
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(width.dp, 600.dp))) {
                val density = LocalDensity.current.density
                CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                    MaterialTheme {
                        labels =
                            listOf(
                                stringResource(R.string.board_dialog_close),
                                "Выбрать иконку поста",
                                stringResource(R.string.composer_sage),
                                stringResource(R.string.composer_thread_author),
                                stringResource(R.string.attachment_add),
                            )
                        Box(Modifier.width(width.dp)) {
                            ComposerScreenHeader(title, headerState(), true, {}, {}, {})
                        }
                    }
                }
            }
        }
        val bounds =
            labels.map { label ->
                val node = compose.onNodeWithContentDescription(label)
                node.assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
                node.fetchSemanticsNode().boundsInRoot
            }
        bounds.forEach { assertEquals(bounds.first().center.y, it.center.y, 1f) }
        bounds.zipWithNext { left, right -> assertTrue(left.right <= right.left + 1f) }
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(title).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertEquals(1, results.single().lineCount)
    }

    private fun headerState() =
        ComposerScreenState(
            ComposerDraftState(),
            PostingRules(identity = PostingIdentityRules(icons = listOf(PostingIcon(1, "Тест")))),
            ComposerCaptchaState(),
            ComposerScreenStatus(ComposerProgressState(), ComposerFailureState(), false),
        )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}dp / font {1} / {2}")
        fun cases(): List<Array<Any>> =
            listOf(320, 393, 600).flatMap { width ->
                listOf(1f, 2f).flatMap { scale ->
                    listOf("Новый тред в /b/", "Новое сообщение в /b/").map { title -> arrayOf(width, scale, title) }
                }
            }
    }
}
