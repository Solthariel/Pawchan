package com.solthariel.pawchan.nativebuild

import java.nio.file.Files
import java.nio.file.Path

internal data class NativeToolchain(
    val bin: Path,
    val api: Int,
) {
    fun compiler(abi: AndroidAbi): String = bin.resolve("${abi.compiler}$api-clang").toString()

    fun tool(name: String): String = bin.resolve("llvm-$name").toString()

    companion object {
        fun discover(
            ndk: Path,
            lock: DependencyLock,
        ): NativeToolchain {
            val properties = java.util.Properties().apply { Files.newInputStream(ndk.resolve("source.properties")).use { load(it) } }
            require(properties.getProperty("Pkg.Revision")?.trim() == lock.ndk) { "Expected NDK ${lock.ndk}" }
            val bin = ndk.resolve("toolchains/llvm/prebuilt/linux-x86_64/bin").toAbsolutePath()
            require(Files.isExecutable(bin.resolve("clang"))) { "Linux x86_64 Android NDK required at $ndk" }
            return NativeToolchain(bin, lock.api)
        }
    }
}

internal data class RecipeContext(
    val directory: Path,
    val prefix: Path,
    val abi: AndroidAbi,
    val toolchain: NativeToolchain,
    val jobs: Int,
)

internal object BuildRecipes {
    fun dav1d(context: RecipeContext): List<Command> {
        val cross = context.directory.resolve("android.cross")
        val compiler = context.toolchain.compiler(context.abi)
        // Meson strings must be quoted independently of shell quoting.
        require('\'' !in compiler && '\'' !in context.toolchain.bin.toString()) { "NDK path cannot contain a single quote" }
        Files.writeString(
            cross,
            """
            [binaries]
            c = '$compiler'
            ar = '${context.toolchain.tool("ar")}'
            strip = '${context.toolchain.tool("strip")}'
            [host_machine]
            system = 'android'
            cpu_family = '${context.abi.mesonFamily}'
            cpu = '${context.abi.cpu}'
            endian = 'little'
            [built-in options]
            c_link_args = ['-Wl,-z,max-page-size=16384']
            """.trimIndent(),
        )
        val options =
            mutableListOf(
                "meson",
                "setup",
                "out",
                "--cross-file",
                cross.toString(),
                "--prefix",
                context.prefix.toString(),
                "--libdir=lib",
                "--default-library=shared",
                "--buildtype=release",
                "-Denable_tools=false",
                "-Denable_tests=false",
            )
        if (context.abi == AndroidAbi.X86 || context.abi == AndroidAbi.X64) options += "-Denable_asm=false"
        return listOf(
            Command(options, context.directory),
            Command(listOf("ninja", "-C", "out", "-j", context.jobs.toString(), "install"), context.directory),
        )
    }

    fun ffmpeg(
        context: RecipeContext,
        dav1dPrefix: Path,
    ): List<Command> {
        val compiler = context.toolchain.compiler(context.abi)
        val args =
            mutableListOf(
                "sh",
                "./configure",
                "--prefix=${context.prefix}",
                "--target-os=android",
                "--enable-cross-compile",
                "--arch=${context.abi.ffmpegArch}",
                "--cc=$compiler",
                "--ld=$compiler",
                "--sysroot=${context.toolchain.bin.parent.resolve("sysroot")}",
                "--ar=${context.toolchain.tool("ar")}",
                "--nm=${context.toolchain.tool("nm")}",
                "--ranlib=${context.toolchain.tool("ranlib")}",
                "--strip=${context.toolchain.tool("strip")}",
                "--pkg-config=pkg-config",
            )
        args +=
            listOf(
                "--disable-autodetect",
                "--disable-static",
                "--enable-shared",
                "--disable-symver",
                "--disable-debug",
                "--disable-doc",
                "--disable-programs",
                "--disable-encoders",
                "--disable-muxers",
                "--disable-protocols",
                "--enable-protocol=file",
                "--disable-network",
                "--enable-decoders",
                "--enable-demuxers",
                "--enable-parsers",
                "--disable-filters",
                "--enable-filter=atempo",
                "--enable-avfilter",
                "--disable-avdevice",
                "--enable-jni",
                "--enable-mediacodec",
                "--enable-libdav1d",
                "--enable-decoder=libdav1d",
                "--extra-ldflags=-Wl,-z,max-page-size=16384",
            )
        if (context.abi == AndroidAbi.ARM32) args += "--cpu=armv7-a"
        if (context.abi == AndroidAbi.X86 || context.abi == AndroidAbi.X64) args += listOf("--enable-pic", "--disable-x86asm")
        val environment =
            mapOf(
                "PKG_CONFIG_LIBDIR" to dav1dPrefix.resolve("lib/pkgconfig").toString(),
                "PKG_CONFIG_PATH" to "",
                "PKG_CONFIG_SYSROOT_DIR" to "",
            )
        return listOf(
            Command(args, context.directory, environment),
            Command(listOf("make", "-j", context.jobs.toString()), context.directory, environment),
            Command(listOf("make", "install"), context.directory, environment),
        )
    }
}
