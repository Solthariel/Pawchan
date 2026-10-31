package com.solthariel.pawchan.update

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/** Only this repository's HTTPS content/asset endpoints may receive the installation's credential. */
internal class GithubUpdateUrls(
    repository: String,
) {
    private val parts = repository.split('/')
    private val api = "https://api.github.com/".toHttpUrl()
    private val apiPrefix = listOf("repos") + parts

    init {
        require(parts.size == 2 && parts.all { it.matches(Regex("[A-Za-z0-9_.-]+")) }) { "Invalid repository" }
    }

    fun content(url: HttpUrl): HttpUrl {
        require(url.isPrivateUpdateHttps()) { "Unsafe update URL" }
        if (isApi(url)) return url
        val segments = url.pathSegments
        val revisionAndPath = segments.drop(parts.size)
        require(url.host == "raw.githubusercontent.com" && segments.take(parts.size) == parts) {
            "Private update content must belong to the configured repository"
        }
        require(revisionAndPath.size >= 2) { "Missing content revision or path" }
        return endpoint(listOf("contents") + revisionAndPath.drop(1))
            .newBuilder()
            .addQueryParameter("ref", revisionAndPath.first())
            .build()
    }

    fun releaseLookup(source: HttpUrl): Pair<HttpUrl, String> {
        require(source.isPrivateUpdateHttps()) { "Unsafe update URL" }
        val segments = source.pathSegments
        val prefix = parts + listOf("releases", "download")
        require(source.host == "github.com" && segments.take(prefix.size) == prefix) { "Unsafe APK source" }
        val asset = segments.drop(prefix.size)
        require(asset.size == 2 && asset.all(String::isNotBlank)) { "Expected a release tag and asset name" }
        return endpoint(listOf("releases", "tags", asset.first())) to asset.last()
    }

    fun asset(id: Long): HttpUrl {
        require(id > 0) { "Invalid asset ID" }
        return endpoint(listOf("releases", "assets", id.toString()))
    }

    fun isApi(url: HttpUrl): Boolean {
        val path = url.pathSegments
        val trustedOrigin = url.isPrivateUpdateHttps() && url.host == api.host
        return trustedOrigin && path.take(apiPrefix.size) == apiPrefix && supportedOperation(path.drop(apiPrefix.size))
    }

    private fun supportedOperation(operation: List<String>): Boolean =
        when (operation.firstOrNull()) {
            "contents" -> operation.size >= 2
            "releases" -> supportedRelease(operation)
            else -> false
        }

    private fun supportedRelease(operation: List<String>): Boolean {
        if (operation.size != RELEASE_OPERATION_SEGMENTS) return false
        return when (operation[1]) {
            "tags" -> operation.last().isNotBlank()
            "assets" -> operation.last().toLongOrNull()?.let { it > 0 } == true
            else -> false
        }
    }

    private fun endpoint(path: List<String>): HttpUrl {
        val builder = api.newBuilder()
        (apiPrefix + path).forEach(builder::addPathSegment)
        return builder.build()
    }
}

/** Disallow credentials in URLs and TLS downgrade/nonstandard ports on every request hop. */
internal fun HttpUrl.isPrivateUpdateHttps(): Boolean =
    isHttps && port == PRIVATE_UPDATE_HTTPS_PORT && username.isEmpty() && password.isEmpty()

private const val PRIVATE_UPDATE_HTTPS_PORT = 443
private const val RELEASE_OPERATION_SEGMENTS = 3
