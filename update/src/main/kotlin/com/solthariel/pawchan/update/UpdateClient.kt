package com.solthariel.pawchan.update

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.coroutines.executeAsync
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

internal class UpdateClient(
    context: Context,
    private val configuration: UpdateConfiguration,
    private val httpClient: OkHttpClient = OkHttpClient(),
) : AutoCloseable {
    private val applicationContext = context.applicationContext
    private val verifier = UpdateVerifier(applicationContext)

    suspend fun check(): Pair<UpdatePackage, String?>? {
        val manifestUrl = requireHttpsUrl(configuration.manifestUrl)
        val release = UpdateManifestDecoder.decodeClientPackage(readText(manifestUrl))
        if (!UpdateChecker.isNewer(configuration.currentVersionCode, release)) {
            return null
        }
        check(release.minSdk <= Build.VERSION.SDK_INT) { "Update requires a newer Android version" }
        val changelog = release.changelog?.let { readText(resolveHttpsUrl(manifestUrl, it)) }
        return release to changelog
    }

    suspend fun download(
        release: UpdatePackage,
        onProgress: (Int) -> Unit,
    ): File =
        withContext(Dispatchers.IO) {
            val source = checkNotNull(release.source) { "Update source is missing" }
            val manifestUrl = requireHttpsUrl(configuration.manifestUrl)
            val downloadUrl = resolveHttpsUrl(manifestUrl, source)
            check(release.length in 1..MAX_APK_BYTES) { "Invalid update length" }
            val updateDirectory = File(applicationContext.cacheDir, UPDATE_DIRECTORY).apply { mkdirs() }
            val target = File(updateDirectory, UPDATE_FILE_NAME)
            val partial = File(updateDirectory, "$UPDATE_FILE_NAME.part")
            partial.delete()
            try {
                downloadTo(downloadUrl, partial, release.length, onProgress)
                verifier.verify(partial, release)
                check(partial.renameTo(target) || copyReplacing(partial, target)) { "Cannot store verified update" }
                target
            } finally {
                partial.delete()
            }
        }

    private suspend fun readText(url: HttpUrl): String {
        val response = httpClient.newCall(Request.Builder().url(url).build()).executeAsync()
        response.use {
            check(it.isSuccessful) { "Update request failed: ${it.code}" }
            return checkNotNull(it.body).string()
        }
    }

    private suspend fun downloadTo(
        url: HttpUrl,
        target: File,
        expectedLength: Long,
        onProgress: (Int) -> Unit,
    ) {
        val response = httpClient.newCall(Request.Builder().url(url).build()).executeAsync()
        response.use {
            check(it.isSuccessful) { "Update download failed: ${it.code}" }
            val body = checkNotNull(it.body)
            val copied = writeBody(body, target, expectedLength, onProgress)
            check(copied == expectedLength) { "Downloaded update length mismatch" }
        }
    }

    private fun writeBody(
        body: ResponseBody,
        target: File,
        expectedLength: Long,
        onProgress: (Int) -> Unit,
    ): Long =
        body.byteStream().use { input ->
            FileOutputStream(target).use { output ->
                copyStream(input, output, expectedLength, onProgress)
            }
        }

    private fun copyStream(
        input: InputStream,
        output: OutputStream,
        expectedLength: Long,
        onProgress: (Int) -> Unit,
    ): Long {
        val buffer = ByteArray(BUFFER_SIZE)
        var copied = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            copied += count
            check(copied <= MAX_APK_BYTES) { "Update exceeds size limit" }
            output.write(buffer, 0, count)
            onProgress(((copied * PERCENT) / expectedLength).coerceIn(0, PERCENT).toInt())
        }
        return copied
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
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
        httpClient.cache?.close()
    }

    private fun copyReplacing(
        source: File,
        target: File,
    ): Boolean =
        runCatching {
            source.copyTo(target, overwrite = true)
            source.delete()
            true
        }.getOrDefault(false)

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
        const val MAX_APK_BYTES = 250L * 1024L * 1024L
        const val PERCENT = 100L
        const val UPDATE_DIRECTORY = "updates"
        const val UPDATE_FILE_NAME = "Pawchan-update.apk"
    }
}
