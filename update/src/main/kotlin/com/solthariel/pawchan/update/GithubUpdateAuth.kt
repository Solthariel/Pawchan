package com.solthariel.pawchan.update

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.HttpURLConnection

/** A network interceptor rechecks EVERY redirect hop, not just the initial request. */
internal class GithubUpdateAuth(
    private val urls: GithubUpdateUrls,
    private val token: () -> String?,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = authorize(chain.request())
        val response = chain.proceed(request)
        if (urls.isApi(request.url) && response.code in ACCESS_DENIED_CODES) {
            response.close()
            throw UpdateAccessRequired()
        }
        return response
    }

    internal fun authorize(request: Request): Request {
        val url = request.url
        requireSafeRequest(request)
        val builder = request.newBuilder().removeHeader("Authorization")
        if (urls.isApi(url)) {
            val credential = token()?.takeIf(String::isNotBlank) ?: throw UpdateAccessRequired()
            val accept =
                when {
                    url.pathSegments[API_OPERATION_INDEX] == "contents" -> "application/vnd.github.raw+json"
                    url.pathSegments.takeLast(2).first() == "assets" -> "application/octet-stream"
                    else -> "application/vnd.github+json"
                }
            return builder
                .header("Authorization", "Bearer $credential")
                .header("Accept", accept)
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("Cache-Control", "no-store")
                .build()
        }
        if (url.host !in setOf("release-assets.githubusercontent.com", "objects.githubusercontent.com")) {
            throw IOException("Unexpected private update redirect")
        }
        return builder.build()
    }

    private fun requireSafeRequest(request: Request) {
        if (!request.url.isPrivateUpdateHttps() || request.method != "GET") {
            throw IOException("Unsafe private update request")
        }
    }
}

internal class UpdateAccessRequired : IOException("Private update access is required")

private const val API_OPERATION_INDEX = 3
private val ACCESS_DENIED_CODES =
    setOf(
        HttpURLConnection.HTTP_UNAUTHORIZED,
        HttpURLConnection.HTTP_FORBIDDEN,
        HttpURLConnection.HTTP_NOT_FOUND,
    )
