package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.catalog.CatalogCardMetadata
import com.solthariel.pawchan.catalog.CatalogThreadAgeUi
import com.solthariel.pawchan.catalog.CatalogThreadContentUi
import com.solthariel.pawchan.catalog.CatalogThreadFlagsUi
import com.solthariel.pawchan.catalog.CatalogThreadStatisticsUi
import com.solthariel.pawchan.catalog.CatalogThreadTextUi
import com.solthariel.pawchan.catalog.CatalogThreadUi
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.navigation.defaultBoardAddress
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CatalogCompactMetadataUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun countsUseIconsWithAccessibleNamesAndAgeIsOnTheRight() {
        val thread =
            CatalogThreadUi(
                ThreadAddress(defaultBoardAddress(), requireNotNull(ThreadId.parse(42))),
                CatalogThreadContentUi(null, CatalogThreadTextUi("ОП"), null, null, false),
                emptyList(),
                CatalogThreadFlagsUi(false, false, false),
                CatalogThreadStatisticsUi(17, 2, 2_000_000L, true),
                age = CatalogThreadAgeUi(1_800_000_000L, "5 мин назад"),
            )
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(220.dp)) { CatalogCardMetadata(thread) }
            }
        }
        val replies = compose.onNodeWithContentDescription("Ответов: 17")
        val age = compose.onNodeWithText("5 мин назад")
        replies.assertIsDisplayed()
        compose.onNodeWithContentDescription("Файлов: 2").assertIsDisplayed()
        compose.onNodeWithText("2.0 МБ").assertIsDisplayed()
        age.assertIsDisplayed()
        compose.onNodeWithText("Ответов: 17").assertDoesNotExist()
        compose.onNodeWithText("Файлов: 2").assertDoesNotExist()
        assertTrue(age.fetchSemanticsNode().boundsInRoot.left > replies.fetchSemanticsNode().boundsInRoot.right)
    }
}
