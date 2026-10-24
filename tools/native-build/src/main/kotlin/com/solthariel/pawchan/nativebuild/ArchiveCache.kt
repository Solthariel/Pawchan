package com.solthariel.pawchan.nativebuild

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

internal fun interface ArchiveTransport {
    fun download(
        uri: URI,
        destination: Path,
    )
}

internal class HttpArchiveTransport :
    ArchiveTransport,
    AutoCloseable {
    private val client =
        HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()

    override fun download(
        uri: URI,
        destination: Path,
    ) {
        val request =
            HttpRequest
                .newBuilder(uri)
                .timeout(Duration.ofMinutes(3))
                .GET()
                .build()
        val pending = client.sendAsync(request, HttpResponse.BodyHandlers.ofFile(destination))
        try {
            val response = pending.get(180, TimeUnit.SECONDS)
            if (response.statusCode() != 200) throw SourceUnavailable("HTTP ${response.statusCode()}: $uri")
        } catch (failure: ExecutionException) {
            throw SourceUnavailable("Cannot download $uri", failure.cause)
        } catch (failure: TimeoutException) {
            throw SourceUnavailable("Download timed out: $uri", failure)
        } finally {
            if (!pending.isDone) pending.cancel(true)
        }
    }

    override fun close() {
        client.close()
    }
}

internal class ArchiveCache(
    private val root: Path,
    private val transport: ArchiveTransport,
) {
    fun obtain(
        spec: DependencySpec,
        offline: Boolean,
    ): Path {
        Files.createDirectories(root)
        val cached = root.resolve("${spec.sha256}.archive")
        if (Files.isRegularFile(cached)) {
            if (FileIntegrity.digest(cached) == spec.sha256) return cached
            if (offline) throw IntegrityFailure("Corrupted offline cache: $cached")
            Files.delete(cached)
        }
        if (offline) throw SourceUnavailable("No verified offline archive for ${spec.name}")
        val staged = Files.createTempFile(root, "download-", ".part")
        try {
            transport.download(spec.url, staged)
            if (FileIntegrity.digest(staged) != spec.sha256) throw IntegrityFailure("SHA-256 mismatch: ${spec.name}")
            Files.move(staged, cached, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            return cached
        } finally {
            Files.deleteIfExists(staged)
        }
    }
}
