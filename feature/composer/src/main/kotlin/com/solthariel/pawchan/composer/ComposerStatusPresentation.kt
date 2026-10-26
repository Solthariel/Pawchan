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
    cooldownSeconds: Long? = null,
): ComposerStatusPresentation {
    val failed = failure.error != null || !failure.reason.isNullOrBlank()
    val label =
        when {
            progress.submitting -> {
                uploadStatusText(progress.upload)
            }

            progress.sent -> {
                "Сообщение отправлено"
            }

            failure.retryDeadlineElapsedMillis != null -> {
                when (cooldownSeconds) {
                    null -> "Сервер ограничил запросы"
                    0L -> "Ожидание завершено. Можно повторить загрузку капчи"
                    else -> "Сервер ограничил запросы. Повторить через $cooldownSeconds с"
                }
            }

            failed -> {
                composerFailureText(failure)
            }

            progress.captchaSelecting -> {
                "Проверка капчи…"
            }

            progress.captchaLoading -> {
                "Загрузка капчи…"
            }

            captchaSatisfied -> {
                captchaSolvedLabel
            }

            else -> {
                "Решите капчу"
            }
        }
    return ComposerStatusPresentation(label, failed && !progress.submitting && !progress.sent)
}

private fun composerFailureText(failure: ComposerFailureState): String =
    when (failure.error) {
        ReplyError.CAPTCHA -> "Капча недействительна"
        ReplyError.NETWORK -> "Нет соединения с 2ch"
        ReplyError.TIMEOUT -> "Сервер не отвечает"
        ReplyError.SECURE_CONNECTION -> "Нет защищённого соединения"
        ReplyError.SUBMISSION_UNCONFIRMED -> SUBMISSION_UNCONFIRMED_TEXT
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

internal fun ComposerStatusPresentation.withUploadBudget(
    budget: ComposerUploadBudget?,
    progress: ComposerProgressState,
): ComposerStatusPresentation {
    val message = budget?.status
    return if (message != null && !progress.submitting && !progress.sent) {
        ComposerStatusPresentation(message, budget.remaining?.let { it < 0 } == true || budget.problem != null)
    } else {
        this
    }
}
