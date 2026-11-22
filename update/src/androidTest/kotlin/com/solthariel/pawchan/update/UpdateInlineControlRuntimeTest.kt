package com.solthariel.pawchan.update

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class UpdateInlineControlRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun discoveryDownloadProgressAndInstallRemainSeparate() {
        val client = InlineFakeOperations()
        val model = UpdateViewModel(client)
        val controller = UpdatePanelController(model, UpdateConfiguration("https://example.org/feed", 1, "1"))
        compose.setContent { MaterialTheme { UpdateInlineControl(controller) { _, _ -> } } }
        compose.onNodeWithText("Проверить обновления").performClick()
        compose.onNodeWithText("Проверяем обновления…").assertIsDisplayed().assertIsNotEnabled()
        compose.runOnIdle { client.check.complete(client.release to null) }
        compose.onNodeWithText("Скачать обновление").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, client.downloadCount) }
        compose.onNodeWithText("Скачать обновление").performClick()
        compose.onNodeWithText("Загрузка: 50%").assertIsDisplayed().assertIsNotEnabled()
        compose.runOnIdle { client.download.complete(File("verified.apk")) }
        compose.onNodeWithText("Установить обновление").assertIsDisplayed()
    }

    @Test fun missingFeedShowsNoticeAndAllowsRetryWithoutDialog() {
        val controller = UpdatePanelController(UpdateViewModel(InlineFakeOperations()), UpdateConfiguration("", 1, "1"))
        compose.setContent {
            MaterialTheme {
                UpdateInlineControl(controller) { message, _ -> if (message != null) Text(message) }
            }
        }
        compose.onNodeWithText("Проверить обновления").performClick()
        compose.onNodeWithText("Адрес сервера обновлений ещё не настроен.").assertIsDisplayed()
        compose.onNodeWithText("Проверить обновления").assertIsDisplayed()
        compose.onNodeWithText("Проверка обновлений").assertDoesNotExist()
    }
}

private class InlineFakeOperations : UpdateOperations {
    val release =
        UpdatePackage().apply {
            versionCode = 2
            versionName = "2"
        }
    val check = CompletableDeferred<Pair<UpdatePackage, String?>?>()
    val download = CompletableDeferred<File>()
    var downloadCount = 0

    override suspend fun check(): Pair<UpdatePackage, String?>? = check.await()

    override suspend fun download(
        release: UpdatePackage,
        onProgress: (Int) -> Unit,
    ): File {
        downloadCount++
        onProgress(50)
        return download.await()
    }

    override fun close() = Unit
}
