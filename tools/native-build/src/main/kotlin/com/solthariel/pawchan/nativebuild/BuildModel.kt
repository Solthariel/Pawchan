package com.solthariel.pawchan.nativebuild

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.nio.file.Path

internal data class GitSource(
    val url: String,
    val ref: String,
    val commit: String,
)

internal data class DependencySpec(
    val name: String,
    val version: String,
    val url: URI,
    val sha256: String,
    val git: GitSource?,
) {
    init {
        require(name.matches(Regex("[a-z][a-z0-9]*")))
        require(sha256.matches(Regex("[a-f0-9]{64}")))
        require(url.scheme == "https")
        require(
            git == null ||
                (
                    git.url.startsWith("https://") && git.ref.startsWith("refs/") && git.ref.none(Char::isWhitespace) &&
                        git.commit.matches(Regex("[a-f0-9]{40}"))
                ),
        )
    }
}

internal data class DependencyLock(
    val ndk: String,
    val api: Int,
    val dependencies: List<DependencySpec>,
) {
    companion object {
        fun read(path: Path): DependencyLock {
            val document = Json.parseToJsonElement(path.toFile().readText()).jsonObject
            val dependencies =
                document.getValue("dependencies").jsonArray.map { entry ->
                    val fields = entry.jsonObject
                    val git =
                        fields["git"]?.jsonObject?.let {
                            GitSource(
                                it.getValue("url").jsonPrimitive.content,
                                it.getValue("ref").jsonPrimitive.content,
                                it.getValue("commit").jsonPrimitive.content,
                            )
                        }
                    DependencySpec(
                        fields.getValue("name").jsonPrimitive.content,
                        fields.getValue("version").jsonPrimitive.content,
                        URI(fields.getValue("url").jsonPrimitive.content),
                        fields.getValue("sha256").jsonPrimitive.content,
                        git,
                    )
                }
            require(dependencies.map { it.name }.toSet() == setOf("dav1d", "ffmpeg") && dependencies.size == 2)
            return DependencyLock(
                document.getValue("ndk").jsonPrimitive.content,
                document
                    .getValue("api")
                    .jsonPrimitive.content
                    .toInt(),
                dependencies,
            )
        }
    }
}

internal enum class AndroidAbi(
    val compiler: String,
    val ffmpegArch: String,
    val mesonFamily: String,
    val cpu: String,
) {
    ARM64("aarch64-linux-android", "aarch64", "aarch64", "aarch64"),
    ARM32("armv7a-linux-androideabi", "arm", "arm", "armv7a"),
    X86("i686-linux-android", "x86", "x86", "i686"),
    X64("x86_64-linux-android", "x86_64", "x86_64", "x86_64"),
    ;

    val id: String get() =
        when (this) {
            ARM64 -> "arm64-v8a"
            ARM32 -> "armeabi-v7a"
            X86 -> "x86"
            X64 -> "x86_64"
        }

    companion object {
        fun parse(value: String): AndroidAbi = entries.firstOrNull { it.id == value } ?: error("Unsupported ABI: $value")
    }
}

internal data class BuildOptions(
    val root: Path,
    val ndk: Path?,
    val abis: List<AndroidAbi>,
    val jobs: Int,
    val offline: Boolean,
) {
    init {
        require(jobs in 1..128)
        require(abis.isNotEmpty() && abis.distinct() == abis)
    }

    val tool: Path get() = root.resolve("tools/native-build")
    val work: Path get() = root.resolve("video-ffmpeg/build/native-rebuild")
}

internal class IntegrityFailure(
    message: String,
) : IllegalStateException(message)

internal class SourceUnavailable(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
