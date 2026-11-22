package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.archive.ArchiveSubmission
import com.solthariel.pawchan.domain.archive.ExternalThreadArchive
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request

/**
 * Добавление через ту же форму, что и на сайте: капча-картинка привязана к сессии (cookie), без логина.
 * Без регистрации Архивач принимает до 100 тредов в день.
 */
class ArhivachApi internal constructor(
    private val calls: Call.Factory,
    private val decodeDispatcher: CoroutineDispatcher,
) : ExternalThreadArchive {
    constructor(calls: Call.Factory) : this(calls, Dispatchers.Default)

    override fun supports(address: ThreadAddress): Boolean = address.board.isDvachSource()

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
        if (!supports(address)) return ArchiveSubmission.Rejected("Этот источник не поддерживается")
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
        if (!supports(address)) return unsupportedDvachSource()
        return if (!validArhivachUrl(url)) {
            DomainOutcome.Failure(DomainFailure.InvalidResponse)
        } else {
            decodeArchiveInBackground(
                decodeDispatcher,
                payload = { calls.arhivachPayload(Request.Builder().url(url).build()) },
                parse = { decodeArhivachThread(address, url, it) },
            )
        }
    }
}

fun validArhivachUrl(raw: String): Boolean {
    val url = raw.toHttpUrlOrNull() ?: return false
    return url.scheme == "https" && url.host == "arhivach.vc" && url.port == HTTPS_PORT &&
        url.username.isEmpty() && url.password.isEmpty() && ARCHIVE_PATH.matches(url.encodedPath) &&
        url.query == null && url.fragment == null
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
    val document = org.jsoup.Jsoup.parseBodyFragment(html)
    val existing = document.text().contains(ALREADY_ADDED)
    val errors =
        document
            .select(".alert-error")
            .map { node ->
                node.select("button.close").remove()
                node.text().trim()
            }.filter(String::isNotEmpty)
    val link =
        when {
            code in HTTP_REDIRECT_START until HTTP_BAD_REQUEST -> {
                archiveThreadLink(location.orEmpty())
            }

            code == HTTP_OK -> {
                val selector = if (existing) "a.thread_title[href]" else ".alert-success a[href]"
                archiveThreadLink(document.selectFirst(selector)?.attr("href").orEmpty())
            }

            else -> {
                null
            }
        }
    return when {
        errors.any { it.contains("капч", ignoreCase = true) } -> ArchiveSubmission.WrongCaptcha("Неверная капча")
        code >= HTTP_BAD_REQUEST -> ArchiveSubmission.Rejected("Архивач ответил ошибкой $code")
        errors.isNotEmpty() && !(existing && link != null) -> ArchiveSubmission.Rejected(errors.joinToString("\n"))
        link != null -> ArchiveSubmission.Accepted(link, alreadyArchived = existing)
        else -> ArchiveSubmission.Rejected("Архивач не вернул ссылку на тред")
    }
}

internal fun archiveThreadLink(text: String): String? {
    val raw = if (ARCHIVE_PATH.matches(text)) "$ARHIVACH_ORIGIN$text" else text
    if (!validArhivachUrl(raw)) return null
    return raw.trimEnd('/') + "/"
}

private const val ARHIVACH_ORIGIN = "https://arhivach.vc"
private const val HTTP_OK = 200
private const val HTTP_REDIRECT_START = 300
private const val HTTP_BAD_REQUEST = 400
private const val ALREADY_ADDED = "Тред уже был добавлен ранее"
private const val HTTPS_PORT = 443
private val ARCHIVE_PATH = Regex("/thread/[1-9][0-9]*/?")
