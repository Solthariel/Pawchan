package com.solthariel.pawchan.nativebuild

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPOutputStream

internal object Fixtures {
    fun write(
        path: Path,
        text: String,
    ): Path {
        Files.createDirectories(path.parent)
        Files.writeString(path, text)
        return path
    }

    fun dependency(
        bytes: ByteArray,
        git: GitSource? = null,
    ): DependencySpec {
        val hash =
            java.security.MessageDigest
                .getInstance("SHA-256")
                .digest(bytes)
                .joinToString("") { "%02x".format(it) }
        return DependencySpec("ffmpeg", "8.1.3", java.net.URI("https://example.test/ffmpeg.tar.gz"), hash, git)
    }

    fun archive(vararg names: String): ByteArray =
        tar(
            names.map { name ->
                TarArchiveEntry(name).apply {
                    size = 4
                    mode = 493
                }
            },
        )

    fun tar(entries: List<TarArchiveEntry>): ByteArray {
        val bytes = ByteArrayOutputStream()
        TarArchiveOutputStream(GZIPOutputStream(bytes)).use { output ->
            entries.forEach { entry ->
                output.putArchiveEntry(entry)
                if (entry.size >
                    0
                ) {
                    output.write(ByteArray(entry.size.toInt()) { 65 })
                }
                output.closeArchiveEntry()
            }
            output.finish()
        }
        return bytes.toByteArray()
    }

    fun elf(
        path: Path,
        abi: AndroidAbi,
        alignment: Long = 16384,
    ) {
        Files.createDirectories(path.parent)
        val wide = abi == AndroidAbi.ARM64 || abi == AndroidAbi.X64
        val header = ByteBuffer.allocate(128).order(ByteOrder.LITTLE_ENDIAN)
        header.putInt(0, 0x464c457f)
        header.put(4, (if (wide) 2 else 1).toByte())
        header.put(5, 1)
        header.putShort(16, 3)
        header.putShort(
            18,
            when (abi) {
                AndroidAbi.ARM64 -> 183
                AndroidAbi.ARM32 -> 40
                AndroidAbi.X86 -> 3
                AndroidAbi.X64 -> 62
            }.toShort(),
        )
        if (wide) {
            header.putLong(32, 64)
            header.putShort(54, 56)
            header.putShort(56, 1)
            header.putLong(112, alignment)
        } else {
            header.putInt(28, 64)
            header.putShort(42, 32)
            header.putShort(44, 1)
            header.putInt(92, alignment.toInt())
        }
        header.putInt(64, 1)
        Files.write(path, header.array())
    }

    fun project(root: Path): BuildOptions {
        val tool = root.resolve("tools/native-build")
        write(
            tool.resolve("native-dependencies.json"),
            """{"ndk":"29.0.14206865","api":30,"dependencies":[
            {"name":"ffmpeg","version":"8.1.3","url":"https://example.test/f","sha256":"${"0".repeat(64)}"},
            {"name":"dav1d","version":"1.5.4","url":"https://example.test/d","sha256":"${"1".repeat(64)}"}]}""",
        )
        write(tool.resolve("src/main/recipe.kt"), "fixture recipe")
        write(tool.resolve("build.gradle.kts"), "fixture build")
        write(root.resolve("video-ffmpeg/src/main/assets/licenses/notice.txt"), "licenses are preserved")
        val ndk = root.resolve("fake-ndk")
        write(ndk.resolve("source.properties"), "Pkg.Revision = 29.0.14206865")
        write(ndk.resolve("toolchains/llvm/prebuilt/linux-x86_64/bin/clang"), "fixture").toFile().setExecutable(true)
        return BuildOptions(root, ndk, listOf(AndroidAbi.ARM64), 3, true)
    }

    fun sources(root: Path): Map<String, Path> {
        write(root.resolve("ffmpeg/configure"), "fixture configure").toFile().setExecutable(true)
        write(root.resolve("ffmpeg/libavformat/mov.c"), "    ff_mov_read_chnl(c->fc, pb, st);\n")
        write(root.resolve("dav1d/meson.build"), "fixture meson")
        return listOf("ffmpeg", "dav1d").associateWith { root.resolve(it) }
    }

    fun installed(
        prefix: Path,
        abi: AndroidAbi,
        ffmpeg: Boolean,
    ) {
        val libraries = if (ffmpeg) BundleManifest.ffmpegLibraries else listOf("dav1d")
        libraries.forEach { elf(prefix.resolve("lib/lib$it.so"), abi) }
        if (ffmpeg) {
            listOf(
                "avformat/avformat.h",
                "avcodec/avcodec.h",
                "avutil/avutil.h",
                "swresample/swresample.h",
                "swscale/swscale.h",
                "avfilter/avfilter.h",
            ).forEach { write(prefix.resolve("include/lib$it"), "fixture header") }
        }
    }
}

internal class SimulatedNativeTools : CommandRunner {
    val commands = mutableListOf<Command>()
    var failInstall = false
    private val prefixes = mutableMapOf<Path, Path>()

    override fun run(command: Command): String {
        commands += command
        val arguments = command.arguments
        if (arguments.first() == "meson") prefixes[command.directory] = Path.of(arguments[arguments.indexOf("--prefix") + 1])
        if ("./configure" in arguments) {
            prefixes[command.directory] = Path.of(arguments.first { it.startsWith("--prefix=") }.substringAfter('='))
        }
        if (arguments.last() == "install") {
            check(!failInstall) { "Simulated compiler failure" }
            val abi =
                AndroidAbi.parse(
                    command.directory.parent.fileName
                        .toString(),
                )
            Fixtures.installed(prefixes.getValue(command.directory), abi, command.directory.fileName.toString() == "ffmpeg")
        }
        return ""
    }
}
