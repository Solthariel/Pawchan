package com.solthariel.pawchan.nativebuild

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

class BuildRecipesTest {
    @get:Rule val temp = TemporaryFolder()

    private fun context(abi: AndroidAbi = AndroidAbi.ARM64): RecipeContext {
        val root = temp.root.toPath()
        return RecipeContext(root, root.resolve("install with spaces"), abi, NativeToolchain(root.resolve("ndk with spaces/bin"), 30), 5)
    }

    @Test fun ffmpegUsesSelectedCompilerAndCorrectSysroot() {
        val args = BuildRecipes.ffmpeg(context(), temp.root.toPath()).first().arguments
        assertTrue(args.any { it.endsWith("aarch64-linux-android30-clang") })
        assertTrue(args.contains("--sysroot=${temp.root.toPath().resolve("ndk with spaces/sysroot")}"))
    }

    @Test fun spacesRemainInsideSingleArgument() {
        val recipe = context()
        assertTrue(
            BuildRecipes
                .ffmpeg(recipe, recipe.prefix)
                .first()
                .arguments
                .contains("--prefix=${recipe.prefix}"),
        )
    }

    @Test fun decoderAndDemuxerCoverageIsBroadWithoutUnneededEncodersOrNetwork() {
        val args = BuildRecipes.ffmpeg(context(), temp.root.toPath()).first().arguments
        assertTrue(
            args.containsAll(
                listOf(
                    "--enable-decoders",
                    "--enable-demuxers",
                    "--enable-parsers",
                    "--enable-mediacodec",
                    "--enable-libdav1d",
                    "--enable-filter=atempo",
                ),
            ),
        )
        assertTrue(args.containsAll(listOf("--disable-encoders", "--disable-muxers", "--disable-network", "--disable-autodetect")))
    }

    @Test fun hostPkgConfigPathsAreNotInherited() {
        val command = BuildRecipes.ffmpeg(context(), temp.root.toPath()).first()
        assertEquals("", command.environment["PKG_CONFIG_PATH"])
        assertEquals("", command.environment["PKG_CONFIG_SYSROOT_DIR"])
        assertEquals(
            temp.root
                .toPath()
                .resolve("lib/pkgconfig")
                .toString(),
            command.environment["PKG_CONFIG_LIBDIR"],
        )
    }

    @Test fun jobsArePassedExplicitlyToBothBuildSystems() {
        assertEquals(listOf("make", "-j", "5"), BuildRecipes.ffmpeg(context(), temp.root.toPath())[1].arguments)
        assertTrue(BuildRecipes.dav1d(context())[1].arguments.containsAll(listOf("-j", "5")))
    }

    @Test fun allPreviouslySupportedAbisHaveExplicitCrossTargets() {
        AndroidAbi.entries.forEach { abi ->
            assertTrue(
                BuildRecipes.ffmpeg(context(abi), temp.root.toPath()).first().arguments.any {
                    it ==
                        "--cc=${context(abi).toolchain.compiler(abi)}"
                },
            )
            BuildRecipes.dav1d(context(abi))
            assertTrue(Files.readString(temp.root.toPath().resolve("android.cross")).contains("cpu_family = '${abi.mesonFamily}'"))
        }
    }

    @Test fun bothRecipesRequestSixteenKiBLinkAlignment() {
        assertTrue(
            BuildRecipes
                .ffmpeg(context(), temp.root.toPath())
                .first()
                .arguments
                .contains("--extra-ldflags=-Wl,-z,max-page-size=16384"),
        )
        BuildRecipes.dav1d(context())
        assertTrue(Files.readString(temp.root.toPath().resolve("android.cross")).contains("max-page-size=16384"))
    }

    @Test fun wrongNdkRevisionIsRejectedBeforeCompile() {
        val options = Fixtures.project(temp.root.toPath())
        Fixtures.write(options.ndk!!.resolve("source.properties"), "Pkg.Revision=28.0.0")
        assertThrows(IllegalArgumentException::class.java) {
            NativeToolchain.discover(options.ndk, DependencyLock.read(options.tool.resolve("native-dependencies.json")))
        }
    }
}
