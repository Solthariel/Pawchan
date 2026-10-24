package com.solthariel.pawchan.nativebuild

import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

internal data class CliRequest(
    val action: String,
    val options: BuildOptions,
    val sources: Map<String, Path>,
    val cache: Path,
)

internal object NativeCli {
    fun parse(
        arguments: Array<String>,
        environment: Map<String, String> = System.getenv(),
        cwd: Path = Path.of("").toAbsolutePath(),
    ): CliRequest {
        require(arguments.isNotEmpty()) { "Usage: plan|prepare|build|verify; see docs/DEVELOPMENT.md for options" }
        require(arguments[0] in setOf("plan", "prepare", "build", "verify")) { "Unknown command: ${arguments[0]}" }
        val flags = flags(arguments.drop(1))
        val root = flags["--project-root"]?.let { cwd.resolve(it).normalize() } ?: locateRoot(cwd)
        val options = options(root, flags, environment, cwd)
        val sources =
            listOf("ffmpeg", "dav1d")
                .mapNotNull { name ->
                    val key = "${name.uppercase()}_SOURCE_DIR"
                    val value = flags["--$name-source"] ?: environment["PAWCHAN_$key"] ?: environment["NEKOCHAN_$key"]
                    value?.let { name to cwd.resolve(it).toAbsolutePath() }
                }.toMap()
        val cache =
            flags["--cache"] ?: environment["PAWCHAN_NATIVE_CACHE"] ?: environment["WEBM_ARCHIVE_CACHE_DIR"]
                ?: "${System.getProperty("user.home")}/.cache/pawchan/native"
        return CliRequest(arguments[0], options, sources, cwd.resolve(cache).toAbsolutePath())
    }

    private fun flags(arguments: List<String>): Map<String, String> {
        val flags = mutableMapOf<String, String>()
        var index = 0
        val allowed = setOf("--project-root", "--ndk", "--abi", "--jobs", "--cache", "--ffmpeg-source", "--dav1d-source")
        while (index < arguments.size) {
            val key = arguments[index++]
            require(key !in flags) { "Duplicate option: $key" }
            if (key == "--offline") {
                flags[key] = "true"
            } else {
                require(key in allowed && index < arguments.size) { "Unknown or incomplete option: $key" }
                flags[key] = arguments[index++]
            }
        }
        return flags
    }

    private fun locateRoot(cwd: Path): Path =
        generateSequence(cwd) { it.parent }
            .firstOrNull { Files.isDirectory(it.resolve("video-ffmpeg/native-prebuilt")) } ?: error("Pass --project-root PATH")

    private fun options(
        root: Path,
        flags: Map<String, String>,
        environment: Map<String, String>,
        cwd: Path,
    ): BuildOptions {
        val ndk =
            flags["--ndk"] ?: environment["ANDROID_NDK_HOME"]
                ?: (environment["ANDROID_HOME"] ?: environment["ANDROID_SDK_ROOT"])?.let { "$it/ndk/29.0.14206865" }
        val abis =
            (flags["--abi"] ?: environment["NATIVE_ABIS"] ?: "arm64-v8a")
                .split(
                    ',',
                    ' ',
                ).filter { it.isNotBlank() }
                .map(AndroidAbi::parse)
        val jobs = (flags["--jobs"] ?: environment["EXTERNAL_CORES"] ?: "4").toInt()
        return BuildOptions(root.toAbsolutePath(), ndk?.let { cwd.resolve(it).toAbsolutePath() }, abis, jobs, "--offline" in flags)
    }
}

fun main(arguments: Array<String>) {
    try {
        val request = NativeCli.parse(arguments)
        HttpArchiveTransport().use { transport ->
            ProcessCommandRunner(request.options.work.resolve("logs")).use { runner ->
                NativePipeline(request.options, runner, transport).execute(request.action, request.sources, request.cache)
            }
        }
    } catch (failure: Exception) {
        System.err.println("Native build failed: ${failure.message}")
        exitProcess(1)
    }
}
