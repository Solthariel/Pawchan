package com.solthariel.pawchan.nativebuild

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.nio.file.Files
import java.nio.file.Path

internal object BundleManifest {
    val ffmpegLibraries = listOf("avutil", "swresample", "swscale", "avcodec", "avformat", "avfilter")

    fun write(
        root: Path,
        lock: DependencyLock,
        options: BuildOptions,
        fingerprint: String,
    ) {
        val hashes = FileIntegrity.files(root).associate { root.relativize(it).toString().replace('\\', '/') to FileIntegrity.digest(it) }
        val manifest =
            buildJsonObject {
                lock.dependencies.forEach { put(it.name, it.version) }
                put("ndk", lock.ndk)
                put("abis", buildJsonArray { options.abis.forEach { add(kotlinx.serialization.json.JsonPrimitive(it.id)) } })
                put("inputFingerprint", fingerprint)
                put("pipeline", "Pawchan Kotlin native build")
                put(
                    "sourceArchives",
                    buildJsonObject {
                        lock.dependencies.forEach { dependency ->
                            put(
                                dependency.name,
                                buildJsonObject {
                                    put("url", dependency.url.toString())
                                    put("sha256", dependency.sha256)
                                },
                            )
                        }
                    },
                )
                put("sha256", buildJsonObject { hashes.forEach { (name, hash) -> put(name, hash) } })
            }
        Files.writeString(root.resolve("manifest.json"), Json { prettyPrint = true }.encodeToString(manifest))
    }

    fun verify(
        root: Path,
        abis: List<AndroidAbi>,
        fingerprint: String? = null,
    ) {
        val manifest = Json.parseToJsonElement(Files.readString(root.resolve("manifest.json"))).jsonObject
        check(manifest.getValue("abis").jsonArray.map { it.jsonPrimitive.content } == abis.map { it.id }) { "ABI list mismatch" }
        if (fingerprint !=
            null
        ) {
            check(manifest.getValue("inputFingerprint").jsonPrimitive.content == fingerprint) { "Build inputs changed" }
        }
        val hashes = manifest.getValue("sha256").jsonObject
        check(hashes.isNotEmpty()) { "Empty native manifest" }
        val actualFiles = FileIntegrity.files(root).map { root.relativize(it).toString().replace('\\', '/') }.toSet() - "manifest.json"
        check(hashes.keys == actualFiles) { "Missing or untracked bundle files" }
        hashes.forEach { (name, hash) ->
            val relative = Path.of(name)
            check(!relative.isAbsolute && relative.none { it.toString() == ".." } && '\\' !in name) { "Invalid manifest path" }
            check(FileIntegrity.digest(root.resolve(relative)) == hash.jsonPrimitive.content) { "Native integrity check failed: $name" }
        }
        abis.forEach { abi -> verifyAbi(root, abi, hashes.keys) }
    }

    private fun verifyAbi(
        root: Path,
        abi: AndroidAbi,
        names: Set<String>,
    ) {
        val libraries = ffmpegLibraries.map { "libraries/ffmpeg/${abi.id}/lib$it.so" } + "libraries/dav1d/${abi.id}/libdav1d.so"
        libraries.forEach { name ->
            check(name in names) { "Missing $name" }
            ElfVerifier.verify(root.resolve(name), abi)
        }
        val headers =
            listOf(
                "avformat/avformat.h",
                "avcodec/avcodec.h",
                "avutil/avutil.h",
                "swresample/swresample.h",
                "swscale/swscale.h",
                "avfilter/avfilter.h",
            )
        headers.forEach { check("external/ffmpeg/include/${abi.id}/lib$it" in names) { "Missing FFmpeg header: $it" } }
    }
}
