package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.arhivach.ArchivePostBody
import com.solthariel.pawchan.arhivach.ArchivePostHeader
import com.solthariel.pawchan.arhivach.toArchiveThreadPresentationUi
import com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences
import com.solthariel.pawchan.core.ui.NO_POST_COLLAPSE
import com.solthariel.pawchan.core.ui.PostDisplayPreferences
import com.solthariel.pawchan.domain.archive.ArchivePost
import com.solthariel.pawchan.domain.archive.ArchivePostDetails
import com.solthariel.pawchan.domain.archive.ArchiveThread
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ArchivePostPresentationRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun archivePostHeaderUsesTheStandardIndexNumberDateAuthorOrder() {
        val post = ArchivePost("338223835", "Аноним", "14.11.26 02:05:15", "Текст", emptyList())

        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalPostDisplayPreferences provides PostDisplayPreferences(showDefaultName = true),
                ) { ArchivePostHeader(1, post) }
            }
        }

        compose.onNodeWithText("1").assertExists()
        compose.onNodeWithText("#338223835").assertExists()
        compose.onNodeWithText("14.11.26 02:05:15").assertExists()
        compose.onNodeWithText("Аноним").assertExists()
        val ordinalLeft =
            compose
                .onNodeWithText("1")
                .fetchSemanticsNode()
                .boundsInRoot
                .left
        val numberLeft =
            compose
                .onNodeWithText("#338223835")
                .fetchSemanticsNode()
                .boundsInRoot
                .left
        val dateLeft =
            compose
                .onNodeWithText("14.11.26 02:05:15")
                .fetchSemanticsNode()
                .boundsInRoot
                .left
        val authorLeft =
            compose
                .onNodeWithText("Аноним")
                .fetchSemanticsNode()
                .boundsInRoot
                .left
        assertTrue(ordinalLeft < numberLeft && numberLeft < dateLeft && dateLeft < authorLeft)
    }

    @Test
    fun archiveHeaderUsesLocalTimeAndCommonOpAndSageMarkers() {
        val post =
            ArchivePost(
                "100",
                null,
                "17/11/26 Втр 00:48:22",
                "text",
                emptyList(),
                details = ArchivePostDetails(timestamp = 1794865702L, originalPoster = true, sage = true),
            )
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalPostDisplayPreferences provides PostDisplayPreferences(zone = java.time.ZoneId.of("Europe/Moscow")),
                ) { ArchivePostHeader(1, post) }
            }
        }
        compose.onNodeWithText("17.11.26 00:48:22").assertExists()
        compose.onNodeWithContentDescription("Создатель треда, Не поднимать").assertExists()
    }

    @Test
    fun archiveStatusUsesPostMarkerAndItsExistingDescription() {
        val post = ArchivePost("1", "Аноним", "14.11.26 02:05:15", "Текст", emptyList())
        val description = "Архивач: исходный тред, возможно, удалён"
        compose.setContent { MaterialTheme { ArchivePostHeader(1, post, "Удалён?") } }

        compose.onNodeWithContentDescription(description).assertExists().performClick()
        compose.onNodeWithText(description).assertExists()
    }

    @Test
    fun archiveStatusFollowsGlobalMarkerVisibility() {
        val post = ArchivePost("1", null, null, "Текст", emptyList())
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalPostDisplayPreferences provides PostDisplayPreferences(showStateMarkers = false),
                ) { ArchivePostHeader(1, post, "активный") }
            }
        }

        compose.onNodeWithContentDescription("Архивач: тред активен и обновляется").assertDoesNotExist()
    }

    @Test
    fun defaultAnonymousNameIsHiddenByTheSameGlobalPreference() {
        val post = ArchivePost("1", "Аноним", null, "Текст", emptyList())
        compose.setContent { MaterialTheme { ArchivePostHeader(1, post) } }

        compose.onNodeWithText("Аноним").assertDoesNotExist()
    }

    @Test
    fun archiveBodyPlacesRealSubjectBeforeTextWithoutPageTitle() {
        val post = ArchivePost("1", null, null, "Текст поста", emptyList(), details = ArchivePostDetails(subject = "Тема поста"))
        val card = ArchiveThread(1, "Отдельный заголовок страницы", listOf(post)).toArchiveThreadPresentationUi().posts.single()
        compose.setContent { MaterialTheme { ArchivePostBody(card) } }

        val subjectTop =
            compose
                .onNodeWithText("Тема поста")
                .fetchSemanticsNode()
                .boundsInRoot.top
        val bodyTop =
            compose
                .onNodeWithText("Текст поста")
                .fetchSemanticsNode()
                .boundsInRoot.top
        assertTrue(subjectTop < bodyTop)
        compose.onNodeWithText("Отдельный заголовок страницы").assertDoesNotExist()
    }

    @Test
    fun archiveBodyDoesNotRepeatTheSubjectWhenItStartsTheText() {
        val post = ArchivePost("1", null, null, "Тема и текст", emptyList(), details = ArchivePostDetails(subject = "Тема"))
        val card = ArchiveThread(1, "Тема — превью", listOf(post)).toArchiveThreadPresentationUi().posts.single()
        compose.setContent { MaterialTheme { ArchivePostBody(card) } }

        compose.onNodeWithText("Тема").assertDoesNotExist()
        compose.onNodeWithText("Тема и текст").assertExists()
    }

    @Test
    fun archiveLongPostUsesTheStandardExpandActionAndThreshold() {
        val body = (1..8).joinToString("\n") { "Строка $it" }
        val post = ArchivePost("1", null, null, body, emptyList())
        val card = ArchiveThread(1, "Тред", listOf(post)).toArchiveThreadPresentationUi().posts.single()
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalPostDisplayPreferences provides PostDisplayPreferences(collapseLines = 2),
                ) { ArchivePostBody(card) }
            }
        }
        val collapsedHeight =
            compose
                .onNodeWithText(body)
                .fetchSemanticsNode()
                .boundsInRoot.height
        compose.onNodeWithText("Раскрыть").performClick()
        compose.onNodeWithText("Раскрыть").assertDoesNotExist()
        val expandedHeight =
            compose
                .onNodeWithText(body)
                .fetchSemanticsNode()
                .boundsInRoot.height
        assertTrue(expandedHeight > collapsedHeight)
    }

    @Test
    fun archiveShortPostHasNoExpandAction() {
        val post = ArchivePost("1", null, null, "Короткий пост", emptyList())
        val card = ArchiveThread(1, "Тред", listOf(post)).toArchiveThreadPresentationUi().posts.single()
        compose.setContent { MaterialTheme { ArchivePostBody(card) } }

        compose.onNodeWithText("Короткий пост").assertExists()
        compose.onNodeWithText("Раскрыть").assertDoesNotExist()
    }

    @Test
    fun archiveRepliesStayExpandedAndOpeningPostCanDisableCollapse() {
        val body = (1..40).joinToString("\n") { "Строка $it" }
        val posts = listOf(ArchivePost("1", null, null, body, emptyList()), ArchivePost("2", null, null, body, emptyList()))
        val cards = ArchiveThread(1, "Тред", posts).toArchiveThreadPresentationUi().posts
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalPostDisplayPreferences provides PostDisplayPreferences(collapseLines = NO_POST_COLLAPSE),
                ) { ArchivePostBody(cards[0]) }
                CompositionLocalProvider(
                    LocalPostDisplayPreferences provides PostDisplayPreferences(collapseLines = 2),
                ) { ArchivePostBody(cards[1]) }
            }
        }
        compose.onNodeWithText("Раскрыть").assertDoesNotExist()
    }
}
