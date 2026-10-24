package com.solthariel.pawchan.nativebuild

import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

internal class NativePipeline(
    private val options: BuildOptions,
    private val runner: CommandRunner,
    private val transport: ArchiveTransport,
) {
    private val lock = DependencyLock.read(options.tool.resolve("native-dependencies.json"))

    fun execute(
        action: String,
        supplied: Map<String, Path>,
        cacheRoot: Path,
    ) {
        require(action in setOf("plan", "prepare", "build", "verify"))
        if (action == "plan") {
            println("Dependencies: ${lock.dependencies.map { "${it.name} ${it.version}" }}; NDK ${lock.ndk}; API ${lock.api}")
            println(
                "ABIs: ${options.abis.map { it.id }}; jobs: ${options.jobs}; offline: ${options.offline}; output: ${options.work.resolve(
                    "bundle",
                )}",
            )
            println(
                "Requires Linux x86_64, JDK 25, NDK, meson, ninja, make, pkg-config and (for fallback) git. No changes to native-prebuilt.",
            )
            return
        }
        Files.createDirectories(options.work)
        FileChannel.open(options.work.resolve("pipeline.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
            val held = channel.tryLock() ?: error("Another native build owns ${options.work}")
            held.use {
                if (action ==
                    "verify"
                ) {
                    BundleManifest.verify(options.work.resolve("bundle"), options.abis)
                } else {
                    prepareAndBuild(action, supplied, cacheRoot)
                }
            }
        }
    }

    private fun prepareAndBuild(
        action: String,
        supplied: Map<String, Path>,
        cacheRoot: Path,
    ) {
        val toolchain =
            if (action == "build") {
                NativeToolchain.discover(requireNotNull(options.ndk) { "Set ANDROID_NDK_HOME or pass --ndk" }, lock)
            } else {
                null
            }
        val store = SourceStore(options.work.resolve("sources"), ArchiveCache(cacheRoot, transport), runner)
        val sources = lock.dependencies.associate { it.name to store.prepare(it, supplied[it.name], options.offline) }
        if (action == "prepare") return
        val ndk = requireNotNull(options.ndk) { "Set ANDROID_NDK_HOME or pass --ndk" }
        val fingerprint = fingerprint(sources, ndk)
        val target = options.work.resolve("bundle")
        if (Files.exists(target.resolve("manifest.json")) &&
            reusable(target, fingerprint)
        ) {
            println("Reusing integrity-checked bundle: $target")
            return
        }
        val staged = Files.createTempDirectory(options.work, "bundle-")
        try {
            options.abis.forEach { abi -> buildAbi(abi, sources, staged, requireNotNull(toolchain)) }
            FileIntegrity.copyTree(options.root.resolve("video-ffmpeg/src/main/assets/licenses"), staged.resolve("licenses"))
            BundleManifest.write(staged, lock, options, fingerprint)
            BundleManifest.verify(staged, options.abis, fingerprint)
            FileIntegrity.replace(staged, target)
            println("Verified bundle: $target. Bundled application libraries were NOT replaced.")
        } finally {
            staged.toFile().deleteRecursively()
        }
    }

    private fun fingerprint(
        sources: Map<String, Path>,
        ndk: Path,
    ): String =
        FileIntegrity.text(
            listOf(
                FileIntegrity.digest(options.tool.resolve("native-dependencies.json")),
                FileIntegrity.tree(options.tool.resolve("src/main")),
                FileIntegrity.digest(options.tool.resolve("build.gradle.kts")),
                FileIntegrity.digest(ndk.resolve("source.properties")),
                options.abis.joinToString { it.id },
                sources.toSortedMap().mapValues { FileIntegrity.tree(it.value) }.toString(),
            ).joinToString("\n"),
        )

    private fun reusable(
        target: Path,
        fingerprint: String,
    ): Boolean =
        try {
            BundleManifest.verify(target, options.abis, fingerprint)
            true
        } catch (_: IllegalStateException) {
            false
        }

    private fun buildAbi(
        abi: AndroidAbi,
        sources: Map<String, Path>,
        bundle: Path,
        toolchain: NativeToolchain,
    ) {
        val work = options.work.resolve("compile/${abi.id}")
        check(work.toFile().deleteRecursively())
        Files.createDirectories(work)
        val dav1d = work.resolve("dav1d")
        val ffmpeg = work.resolve("ffmpeg")
        FileIntegrity.copyTree(sources.getValue("dav1d"), dav1d)
        FileIntegrity.copyTree(sources.getValue("ffmpeg"), ffmpeg)
        val dav1dPrefix = work.resolve("dav1d-install")
        val ffmpegPrefix = work.resolve("ffmpeg-install")
        BuildRecipes.dav1d(RecipeContext(dav1d, dav1dPrefix, abi, toolchain, options.jobs)).forEach { runner.run(it) }
        MovReturnValueFix.apply(ffmpeg)
        BuildRecipes.ffmpeg(RecipeContext(ffmpeg, ffmpegPrefix, abi, toolchain, options.jobs), dav1dPrefix).forEach { runner.run(it) }
        publishLibraries(dav1dPrefix, bundle.resolve("libraries/dav1d/${abi.id}"), listOf("dav1d"))
        publishLibraries(ffmpegPrefix, bundle.resolve("libraries/ffmpeg/${abi.id}"), BundleManifest.ffmpegLibraries)
        FileIntegrity.copyTree(ffmpegPrefix.resolve("include"), bundle.resolve("external/ffmpeg/include/${abi.id}"))
    }

    private fun publishLibraries(
        prefix: Path,
        target: Path,
        names: List<String>,
    ) {
        Files.createDirectories(target)
        names.forEach { name ->
            val library = prefix.resolve("lib/lib$name.so")
            check(Files.isRegularFile(library)) { "Build did not produce $library" }
            Files.copy(library, target.resolve(library.fileName))
        }
    }
}
