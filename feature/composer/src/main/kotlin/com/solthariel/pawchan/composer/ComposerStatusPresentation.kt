package com.solthariel.pawchan.composer

internal data class ComposerStatusPresentation(
    val label: String,
    val failed: Boolean,
)

internal fun composerStatusText(
    progress: ComposerProgressState,
    failure: ComposerFailureState,
    captchaSatisfied: Boolean,
    captchaSolvedLabel: String = "Капча решена",
): ComposerStatusPresentation {
    val failed = failure.error != null || !failure.reason.isNullOrBlank()
    val label =
        when {
            progress.submitting -> "Отправка…"
            progress.sent -> "Сообщение отправлено"
            failed -> composerFailureText(failure)
            progress.captchaSelecting -> "Проверка капчи…"
            progress.captchaLoading -> "Загрузка капчи…"
            captchaSatisfied -> captchaSolvedLabel
            else -> "Решите капчу"
        }
    return ComposerStatusPresentation(label, failed && !progress.submitting && !progress.sent)
}

private fun composerFailureText(failure: ComposerFailureState): String =
    when (failure.error) {
        ReplyError.CAPTCHA -> "Капча недействительна"
        ReplyError.NETWORK -> "Нет соединения с 2ch"
        ReplyError.TIMEOUT -> "Сервер не отвечает"
        ReplyError.SECURE_CONNECTION -> "Нет защищённого соединения"
        ReplyError.INVALID_RESPONSE -> "Некорректный ответ 2ch"
        ReplyError.THREAD_CLOSED -> "Тред закрыт"
        ReplyError.SERVER -> failure.reason.singleStatusLine() ?: "Сбой сервера 2ch"
        ReplyError.REJECTED, null -> failure.reason.singleStatusLine() ?: "2ch отклонил сообщение"
    }

private fun String?.singleStatusLine(): String? =
    this
        ?.replace('\r', ' ')
        ?.replace('\n', ' ')
        ?.trim()
        ?.takeIf(String::isNotEmpty)

internal const val COMPOSER_SUCCESS_DISPLAY_MILLIS = 1000L
