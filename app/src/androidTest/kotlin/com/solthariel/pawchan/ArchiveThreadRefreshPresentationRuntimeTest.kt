package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.solthariel.pawchan.arhivach.ArchiveThreadPresentationUi
import com.solthariel.pawchan.arhivach.rememberArchivePostListData
import com.solthariel.pawchan.arhivach.toArchiveThreadPresentationUi
import com.solthariel.pawchan.domain.archive.ArchivePost
import com.solthariel.pawchan.domain.archive.ArchiveThread
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test

class ArchiveThreadRefreshPresentationRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun sameThreadKeepsPostsUntilUpdatedPresentationIsReady() {
        var thread by mutableStateOf(thread(55, "Старый пост"))
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val prepare: suspend (ArchiveThread) -> ArchiveThreadPresentationUi = {
            if (it.posts.first().text == "Новый пост") {
                started.complete(Unit)
                finish.await()
            }
            it.toArchiveThreadPresentationUi()
        }
        compose.setContent {
            MaterialTheme {
                val data = rememberArchivePostListData(thread, "", prepare)
                data.posts.forEach { Text(it.post.text) }
            }
        }
        compose.waitUntil(5_000) {
            compose
                .onAllNodes(
                    hasText("Старый пост"),
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.runOnIdle { thread = thread(55, "Новый пост") }
        compose.waitUntil(5_000) { started.isCompleted }
        compose.onNodeWithText("Старый пост").assertIsDisplayed()
        compose.onNodeWithText("Новый пост").assertDoesNotExist()
        compose.runOnIdle { finish.complete(Unit) }
        compose.waitUntil(5_000) {
            compose
                .onAllNodes(
                    hasText("Новый пост"),
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithText("Старый пост").assertDoesNotExist()
    }

    @Test fun differentThreadDoesNotShowPreviousThreadWhilePreparing() {
        var thread by mutableStateOf(thread(55, "Первый тред"))
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val prepare: suspend (ArchiveThread) -> ArchiveThreadPresentationUi = {
            if (it.id == 56L) {
                started.complete(Unit)
                finish.await()
            }
            it.toArchiveThreadPresentationUi()
        }
        compose.setContent {
            MaterialTheme {
                val data = rememberArchivePostListData(thread, "", prepare)
                data.posts.forEach { Text(it.post.text) }
            }
        }
        compose.waitUntil(5_000) {
            compose
                .onAllNodes(
                    hasText("Первый тред"),
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.runOnIdle { thread = thread(56, "Второй тред") }
        compose.waitUntil(5_000) { started.isCompleted }
        compose.onNodeWithText("Первый тред").assertDoesNotExist()
        compose.onNodeWithText("Второй тред").assertDoesNotExist()
        compose.runOnIdle { finish.complete(Unit) }
        compose.waitUntil(5_000) {
            compose
                .onAllNodes(
                    hasText("Второй тред"),
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun thread(
        id: Long,
        text: String,
    ) = ArchiveThread(id, "Тред", listOf(ArchivePost("1", null, null, text, emptyList())))
}
