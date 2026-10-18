package com.solthariel.pawchan

import com.solthariel.pawchan.composer.ComposerFailureState
import com.solthariel.pawchan.composer.ComposerProgressState
import com.solthariel.pawchan.composer.ReplyError
import com.solthariel.pawchan.composer.composerStatusText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposerStatusTextTest {
    @Test
    fun normalStatesAreShownInTheSameStatusContract() {
        assertEquals("Решите капчу", text())
        assertEquals("Капча решена", composerStatusText(ComposerProgressState(), ComposerFailureState(), true).label)
        assertEquals("Загрузка капчи…", text(ComposerProgressState(captchaLoading = true)))
        assertEquals("Проверка капчи…", text(ComposerProgressState(captchaSelecting = true)))
        assertEquals("Отправка…", text(ComposerProgressState(submitting = true)))
        assertEquals("Сообщение отправлено", text(ComposerProgressState(sent = true)))
    }

    @Test
    fun solvedCaptchaUsesTheLabelProvidedByTheAndroidResource() {
        val display = composerStatusText(ComposerProgressState(), ComposerFailureState(), true, "Solved resource")
        assertEquals("Solved resource", display.label)
        assertFalse(display.failed)
    }

    @Test
    fun knownErrorsShowSpecificCausesNotAGenericError() {
        val expected =
            mapOf(
                ReplyError.CAPTCHA to "Капча недействительна",
                ReplyError.NETWORK to "Нет соединения с 2ch",
                ReplyError.TIMEOUT to "Сервер не отвечает",
                ReplyError.THREAD_CLOSED to "Тред закрыт",
                ReplyError.SECURE_CONNECTION to "Нет защищённого соединения",
                ReplyError.INVALID_RESPONSE to "Некорректный ответ 2ch",
                ReplyError.SERVER to "Сбой сервера 2ch",
                ReplyError.REJECTED to "2ch отклонил сообщение",
            )
        expected.forEach { (error, label) ->
            val display = composerStatusText(ComposerProgressState(), ComposerFailureState(error), false)
            assertEquals(label, display.label)
            assertTrue(display.failed)
        }
    }

    @Test
    fun captchaFailureRemainsVisibleDuringAutomaticReload() {
        assertEquals(
            "Капча недействительна",
            composerStatusText(
                ComposerProgressState(captchaLoading = true),
                ComposerFailureState(ReplyError.CAPTCHA),
                false,
            ).label,
        )
    }

    @Test
    fun serverReasonIsKeptForTheUiToEllipsizeAndNormalizedToOneLine() {
        val reason = "Файл слишком большой.\n" + "Подробная причина ".repeat(20)
        val display =
            composerStatusText(ComposerProgressState(), ComposerFailureState(ReplyError.REJECTED, reason), false)
        assertEquals(reason.replace('\n', ' ').trim(), display.label)
        assertTrue(display.failed)
    }

    @Test
    fun blankReasonFallsBackAndReasonWithoutAnErrorIsStillShown() {
        assertEquals("2ch отклонил сообщение", text(failure = ComposerFailureState(ReplyError.REJECTED, "  ")))
        assertEquals("Ограничение отправки", text(failure = ComposerFailureState(reason = "Ограничение отправки")))
    }

    @Test
    fun progressAndSuccessOverrideStaleFailuresWithoutErrorColor() {
        for (progress in listOf(ComposerProgressState(submitting = true), ComposerProgressState(sent = true))) {
            assertFalse(composerStatusText(progress, ComposerFailureState(ReplyError.NETWORK), false).failed)
        }
    }

    private fun text(
        progress: ComposerProgressState = ComposerProgressState(),
        failure: ComposerFailureState = ComposerFailureState(),
    ): String = composerStatusText(progress, failure, false).label
}
