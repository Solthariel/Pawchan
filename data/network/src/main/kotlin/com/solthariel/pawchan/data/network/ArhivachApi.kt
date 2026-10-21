package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.archive.ArchiveSubmission
import com.solthariel.pawchan.domain.archive.ExternalThreadArchive
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request

/**
 * Добавление через ту же форму, что и на сайте: капча-картинка привязана к сессии (cookie), без логина.
 * Без регистрации Архивач принимает до 100 тредов в день.
 */
class ArhivachApi(
    private val calls: Call.Factory,
) : ExternalThreadArchive {
    override suspend fun captcha(): DomainOutcome<ByteArray> {
        val request =
            Request
                .Builder()
                .url("$ARHIVACH_ORIGIN/captcha?${System.nanoTime()}")
                .build()
        return when (val result = calls.arhivachExchange(request)) {
            is DomainOutcome.Success -> {
                if (result.value.code == HTTP_OK && result.value.bytes.isNotEmpty()) {
                    DomainOutcome.Success(result.value.bytes)
                } else {
                    DomainOutcome.Failure(DomainFailure.HttpStatus(result.value.code))
                }
            }

            is DomainOutcome.Failure -> {
                result
            }
        }
    }

    override suspend fun add(
        address: ThreadAddress,
        captcha: String,
    ): ArchiveSubmission {
        if (!address.board.isDvachSource()) return ArchiveSubmission.Rejected("Этот источник не поддерживается")
        val form =
            FormBody
                .Builder()
                .add("url", sourceThreadUrl(address))
                .add("add_collapsed", "on")
                .add("captcha_code", captcha.trim())
                .add("submit", "Добавить")
                .build()
        val request =
            Request
                .Builder()
                .url("$ARHIVACH_ORIGIN/add/")
                .post(form)
                .build()
        return when (val result = calls.arhivachExchange(request)) {
            is DomainOutcome.Success -> {
                decodeFormSubmission(
                    result.value.code,
                    result.value.location,
                    result.value.bytes.toString(Charsets.UTF_8),
                )
            }

            is DomainOutcome.Failure -> {
                ArchiveSubmission.Rejected("Архивач недоступен")
            }
        }
    }

    override suspend fun available(): Boolean {
        val request =
            Request
                .Builder()
                .url("$ARHIVACH_ORIGIN/faq/")
                .build()
        val result = calls.arhivachExchange(request)
        return result is DomainOutcome.Success && result.value.code in HTTP_OK until HTTP_BAD_REQUEST
    }

    override suspend fun load(
        address: ThreadAddress,
        url: String,
    ): DomainOutcome<ThreadSnapshot> {
        if (!address.board.isDvachSource()) return unsupportedDvachSource()
        return if (!validArhivachUrl(url)) {
            DomainOutcome.Failure(DomainFailure.InvalidResponse)
        } else {
            when (val result = calls.arhivachPayload(Request.Builder().url(url).build())) {
                is DomainOutcome.Success -> {
                    decodeArhivachThread(address, url, result.value)?.let { DomainOutcome.Success(it) }
                        ?: DomainOutcome.Failure(DomainFailure.InvalidResponse)
                }

                is DomainOutcome.Failure -> {
                    result
                }
            }
        }
    }
}

fun validArhivachUrl(raw: String): Boolean {
    val url = raw.toHttpUrlOrNull() ?: return false
    return url.scheme == "https" && url.host == "arhivach.vc" && url.port == HTTPS_PORT &&
        url.username.isEmpty() && url.password.isEmpty() && ARCHIVE_PATH.matches(url.encodedPath) && url.query == null
}

internal fun sourceThreadUrl(address: ThreadAddress): String =
    "https://2ch.su/${address.board.board.value}/res/${address.thread.value}.html"

/**
 * Ответ формы: перенаправление или страница со ссылкой на тред — принят; «Ошибка ввода капчи» — новая
 * попытка; любая другая ошибка показывается как есть.
 */
internal fun decodeFormSubmission(
    code: Int,
    location: String?,
    html: String,
): ArchiveSubmission {
    val existing = html.contains(ALREADY_ADDED)
    // Ссылка на копию: редирект после добавления или блок треда под «Тред уже был добавлен ранее».
    val link =
        archiveThreadLink(location.orEmpty())
            ?: THREAD_TITLE_LINK.find(html)?.let { archiveThreadLink(it.groupValues[1]) }
            ?: archiveThreadLink(html)
    val errors =
        ALERT_ERROR
            .findAll(html)
            .map { alertText(it.groupValues[1]) }
            .filter(String::isNotBlank)
            .toList()
    return when {
        link != null -> ArchiveSubmission.Accepted(link, alreadyArchived = existing)
        errors.any { it.contains("капч", ignoreCase = true) } -> ArchiveSubmission.WrongCaptcha("Неверная капча")
        errors.isNotEmpty() -> ArchiveSubmission.Rejected(errors.joinToString("\n"))
        code >= HTTP_BAD_REQUEST -> ArchiveSubmission.Rejected("Архивач ответил ошибкой $code")
        else -> ArchiveSubmission.Rejected("Архивач не вернул ссылку на тред")
    }
}

internal fun archiveThreadLink(text: String): String? {
    val number = THREAD_LINK.find(text)?.groupValues?.get(1) ?: return null
    return "$ARHIVACH_ORIGIN/thread/$number/"
}

private fun alertText(html: String): String =
    html
        .replace(TAG, " ")
        .replace("&nbsp;", " ")
        .split(WHITESPACE)
        .filter { it.isNotBlank() && it != "x" && it != "×" }
        .joinToString(" ")

private const val ARHIVACH_ORIGIN = "https://arhivach.vc"
private const val HTTP_OK = 200
private const val HTTP_BAD_REQUEST = 400
private val THREAD_LINK = Regex("""(?:https://arhivach\.vc)?/thread/([1-9][0-9]*)""")
private val ALERT_ERROR = Regex("""<div class="alert alert-error"[^>]*>(.*?)</div>""", RegexOption.DOT_MATCHES_ALL)
private val TAG = Regex("<[^>]+>")
private const val ALREADY_ADDED = "Тред уже был добавлен ранее"
private val THREAD_TITLE_LINK = Regex("""<a\s+class="thread_title"[^>]*\shref="([^"]+)"""")
private val WHITESPACE = Regex("\\s+")
private const val HTTPS_PORT = 443
private val ARCHIVE_PATH = Regex("/thread/[1-9][0-9]*/?")
