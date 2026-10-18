package com.solthariel.pawchan

import com.solthariel.pawchan.thread.ThreadExportDocument
import com.solthariel.pawchan.thread.ThreadExportPost
import com.solthariel.pawchan.thread.ThreadExportRequest
import com.solthariel.pawchan.thread.ThreadExportTarget
import com.solthariel.pawchan.thread.ThreadExportWriter
import com.solthariel.pawchan.thread.ThreadShotStyle
import com.solthariel.pawchan.thread.ThreadToolsState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ThreadExportWriterTest {
    @Test fun htmlSuccessReportsOnlyAfterTargetCompletes() =
        runTest {
            val tools = ThreadToolsState(this)
            val target = RecordingExportTarget()
            val writer = ThreadExportWriter(this, tools, StandardTestDispatcher(testScheduler))
            writer.saveHtml(request(), target)
            assertTrue(tools.exporting)
            assertEquals("Сохранение…", tools.notice)
            runCurrent()
            assertEquals("Сохранение…", tools.notice)
            assertEquals(
                1,
                target.html
                    .single()
                    .posts.size,
            )
            target.completed.complete(Unit)
            advanceUntilIdle()
            assertFalse(tools.exporting)
            assertEquals("HTML-копия сохранена. Вложения доступны по ссылкам.", tools.notice)
        }

    @Test fun concurrentRequestDoesNotStartSecondTarget() =
        runTest {
            val tools = ThreadToolsState(this)
            val first = RecordingExportTarget()
            val second = RecordingExportTarget()
            val writer = ThreadExportWriter(this, tools, StandardTestDispatcher(testScheduler))
            writer.saveHtml(request(), first)
            writer.saveShot(request(), second)
            runCurrent()
            assertEquals(1, first.html.size)
            assertTrue(second.shots.isEmpty())
            first.completed.complete(Unit)
            advanceUntilIdle()
        }

    @Test fun screenshotPassesOnlySelectedPostsAndCapturedStyle() =
        runTest {
            val tools = ThreadToolsState(this)
            val target = RecordingExportTarget().apply { completed.complete(Unit) }
            val style = ThreadShotStyle(textSizeSp = 18, accent = 123)
            val request = ThreadExportRequest(toolsContent(listOf(toolsPost(9), toolsPost(3))), setOf(3), style)
            tools.selected = setOf(9)
            ThreadExportWriter(this, tools, StandardTestDispatcher(testScheduler)).saveShot(request, target)
            advanceUntilIdle()
            assertEquals(
                listOf(3),
                target.shots
                    .single()
                    .first
                    .map { it.index },
            )
            assertEquals(style, target.shots.single().second)
            assertEquals("Скриншот выбранных постов сохранён", tools.notice)
            assertFalse(tools.exporting)
        }

    @Test fun ioFailureKeepsMessageAndAllowsNextExport() =
        runTest {
            val tools = ThreadToolsState(this)
            val writer = ThreadExportWriter(this, tools, StandardTestDispatcher(testScheduler))
            val target = RecordingExportTarget().apply { failure = IOException("Нет доступа к файлу") }
            writer.saveHtml(request(), target)
            advanceUntilIdle()
            assertEquals("Нет доступа к файлу", tools.notice)
            assertFalse(tools.exporting)
            target.failure = null
            target.completed.complete(Unit)
            writer.saveHtml(request(), target)
            advanceUntilIdle()
            assertEquals(2, target.html.size)
            assertEquals("HTML-копия сохранена. Вложения доступны по ссылкам.", tools.notice)
        }

    @Test fun platformErrorsUseExistingMessagesAndResetBusyFlag() =
        runTest {
            val errors =
                listOf(
                    SecurityException() to "Нет доступа к выбранному файлу",
                    IllegalArgumentException() to "Не удалось сохранить файл: неподдерживаемые данные",
                    IOException() to "Не удалось сохранить файл",
                )
            for ((error, message) in errors) {
                val tools = ThreadToolsState(this)
                val target = RecordingExportTarget().apply { failure = error }
                ThreadExportWriter(this, tools, StandardTestDispatcher(testScheduler)).saveHtml(request(), target)
                advanceUntilIdle()
                assertEquals(message, tools.notice)
                assertFalse(tools.exporting)
            }
        }

    @Test fun cancellationIsNotReportedAsSuccessOrFileFailure() =
        runTest {
            val tools = ThreadToolsState(this)
            val target = RecordingExportTarget().apply { failure = CancellationException("owner left") }
            ThreadExportWriter(this, tools, StandardTestDispatcher(testScheduler)).saveHtml(request(), target)
            advanceUntilIdle()
            assertFalse(tools.exporting)
            assertEquals("Сохранение…", tools.notice)
        }

    @Test fun routeOwnerCancellationStopsPendingTargetAndResetsBusyFlag() =
        runTest {
            val owner = TestScope(StandardTestDispatcher(testScheduler))
            val tools = ThreadToolsState(owner)
            val target = RecordingExportTarget()
            ThreadExportWriter(owner, tools, StandardTestDispatcher(testScheduler)).saveHtml(request(), target)
            runCurrent()
            assertTrue(tools.exporting)
            owner.cancel()
            runCurrent()
            assertTrue(target.cancelled)
            assertFalse(tools.exporting)
            assertEquals("Сохранение…", tools.notice)
        }

    private fun request(): ThreadExportRequest = ThreadExportRequest(toolsContent(listOf(toolsPost(9))))
}

private class RecordingExportTarget : ThreadExportTarget {
    val completed = CompletableDeferred<Unit>()
    val html = mutableListOf<ThreadExportDocument>()
    val shots = mutableListOf<Pair<List<ThreadExportPost>, ThreadShotStyle>>()
    var failure: Exception? = null
    var cancelled = false

    override suspend fun saveHtml(document: ThreadExportDocument) {
        html += document
        finish()
    }

    override suspend fun saveShot(
        posts: List<ThreadExportPost>,
        style: ThreadShotStyle,
    ) {
        shots += posts to style
        finish()
    }

    private suspend fun finish() {
        failure?.let { throw it }
        try {
            completed.await()
        } catch (error: CancellationException) {
            cancelled = true
            throw error
        }
    }
}
