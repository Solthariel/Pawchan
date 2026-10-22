package com.solthariel.pawchan.update

import android.content.Context
import android.os.Build
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient

internal class UpdateClient(
    context: Context,
    private val configuration: UpdateConfiguration,
    httpClient: OkHttpClient,
) : UpdateOperations {
    private val transport = UpdateTransport(httpClient)
    private val downloads =
        UpdateDownloadStore(
            java.io.File(context.applicationContext.cacheDir, "updates"),
            UpdateVerifier(context.applicationContext)::verify,
        )

    override suspend fun check(): Pair<UpdatePackage, String?>? {
        val manifestUrl = requireHttpsUrl(configuration.manifestUrl)
        val release = UpdateManifestDecoder.decodeClientPackage(transport.readText(manifestUrl))
        if (!UpdateChecker.isNewer(configuration.currentVersionCode, release)) return null
        check(release.minSdk <= Build.VERSION.SDK_INT) { "Update requires a newer Android version" }
        val changelog = release.changelog?.let { transport.readText(resolveHttpsUrl(manifestUrl, it)) }
        return release to changelog
    }

    override suspend fun download(
        release: UpdatePackage,
        onProgress: (Int) -> Unit,
    ): java.io.File {
        val source = checkNotNull(release.source) { "Update source is missing" }
        val url = resolveHttpsUrl(requireHttpsUrl(configuration.manifestUrl), source)
        return downloads.download(release) { partial ->
            transport.downloadTo(url, partial, release.length, onProgress)
        }
    }

    private fun requireHttpsUrl(value: String): HttpUrl {
        val url = value.toHttpUrlOrNull()
        check(url != null && url.isHttps) { "Update URL must use HTTPS" }
        return url
    }

    private fun resolveHttpsUrl(
        base: HttpUrl,
        value: String,
    ): HttpUrl {
        val normalized = if (value.startsWith("//")) "https:$value" else value
        val resolved = normalized.toHttpUrlOrNull() ?: base.resolve(normalized)
        check(resolved != null && resolved.isHttps) { "Update resource URL must use HTTPS" }
        return resolved
    }

    override fun close() {
        transport.close()
    }
}
