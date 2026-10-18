package com.solthariel.pawchan.thread

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

internal class ThreadExportWriter(
    private val scope: CoroutineScope,
    private val tools: ThreadToolsState,
    private val preparationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private var job: Job? = null

    fun saveHtml(
        request: ThreadExportRequest,
        target: ThreadExportTarget,
    ) = execute {
        val document = withContext(preparationDispatcher) { request.content.exportDocument() }
        target.saveHtml(document)
        "HTML-копия сохранена. Вложения доступны по ссылкам."
    }

    fun saveShot(
        request: ThreadExportRequest,
        target: ThreadExportTarget,
    ) = execute {
        val document =
            withContext(preparationDispatcher) {
                request.content.exportDocument(request.selected, request.displayPreferences)
            }
        target.saveShot(document.posts, request.shotStyle)
        "Скриншот выбранных постов сохранён"
    }

    private fun execute(operation: suspend () -> String) {
        if (job?.isActive == true) return
        tools.exporting = true
        tools.notice = "Сохранение…"
        job =
            scope.launch {
                try {
                    tools.notice = operation()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: IOException) {
                    tools.notice = error.message ?: "Не удалось сохранить файл"
                } catch (_: SecurityException) {
                    tools.notice = "Нет доступа к выбранному файлу"
                } catch (_: IllegalArgumentException) {
                    tools.notice = "Не удалось сохранить файл: неподдерживаемые данные"
                } finally {
                    tools.exporting = false
                }
            }
    }
}
