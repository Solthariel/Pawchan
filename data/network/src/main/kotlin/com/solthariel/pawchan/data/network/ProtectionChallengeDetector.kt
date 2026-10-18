package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.RemoteUrl
import okhttp3.Response

internal object ProtectionChallengeDetector {
    fun detect(
        response: Response,
        payload: String,
    ): DomainFailure.ProtectionChallenge? {
        val body = payload.take(BODY_INSPECTION_LIMIT).lowercase()
        val server = response.header(SERVER_HEADER).orEmpty().lowercase()
        val contentType = response.header(CONTENT_TYPE_HEADER).orEmpty().lowercase()
        val provider = detectProvider(body, server, response.header(CLOUDFLARE_RAY_HEADER))
        val challengeStatus = response.code in CHALLENGE_STATUSES
        val challengeDocument = contentType.contains(HTML_MEDIA_TYPE) && hasChallengeMarker(body)
        val challengeDetected =
            challengeDocument || (challengeStatus && response.code != TOO_MANY_REQUESTS_STATUS && provider != null)
        return if (challengeDetected) {
            val url = requireNotNull(RemoteUrl.parse(response.request.url.toString()))
            DomainFailure.ProtectionChallenge(
                provider = provider ?: DEFAULT_PROVIDER,
                verificationUrl = url,
            )
        } else {
            null
        }
    }

    private fun detectProvider(
        body: String,
        server: String,
        cloudflareRay: String?,
    ): String? =
        when {
            cloudflareRay != null || server.contains(CLOUDFLARE) || body.contains(CLOUDFLARE) -> CLOUDFLARE
            server.contains(DDOS_GUARD) || body.contains(DDOS_GUARD) -> DDOS_GUARD
            body.contains(RECAPTCHA) || body.contains(CAPTCHA) -> CAPTCHA
            else -> null
        }

    private fun hasChallengeMarker(body: String): Boolean = CHALLENGE_MARKERS.any(body::contains)
}

private const val FORBIDDEN_STATUS = 403
private const val TOO_MANY_REQUESTS_STATUS = 429
private const val SERVICE_UNAVAILABLE_STATUS = 503
private val CHALLENGE_STATUSES =
    setOf(FORBIDDEN_STATUS, TOO_MANY_REQUESTS_STATUS, SERVICE_UNAVAILABLE_STATUS)
private val CHALLENGE_MARKERS =
    listOf(
        "checking your browser",
        "just a moment",
        "challenge-platform",
        "captcha",
        "cf-chl-",
        "ddos-guard",
    )
private const val BODY_INSPECTION_LIMIT = 64 * 1024
private const val SERVER_HEADER = "Server"
private const val CONTENT_TYPE_HEADER = "Content-Type"
private const val CLOUDFLARE_RAY_HEADER = "CF-Ray"
private const val HTML_MEDIA_TYPE = "text/html"
private const val CLOUDFLARE = "cloudflare"
private const val DDOS_GUARD = "ddos-guard"
private const val RECAPTCHA = "recaptcha"
private const val CAPTCHA = "captcha"
private const val DEFAULT_PROVIDER = "2ch"
